package fr.rhaz.kheyboard

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.Html
import android.widget.LinearLayout
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponse
import com.android.billingclient.api.BillingClient.BillingResponse.OK
import com.android.billingclient.api.BillingClient.SkuType.INAPP
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails
import com.android.billingclient.api.SkuDetailsParams
import fr.rhaz.kheyboard.R.string.app_name
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking
import org.jetbrains.anko.alert
import org.jetbrains.anko.dip
import org.jetbrains.anko.horizontalPadding
import org.jetbrains.anko.linearLayout
import org.jetbrains.anko.textView
import org.jetbrains.anko.verticalLayout
import org.jetbrains.anko.verticalPadding
import java.io.IOException

val skuList = listOf("sku_small", "sku_medium", "sku_large", "sku_xlarge", "sku_xxlarge")

class Billing : AppCompatActivity() {
    lateinit var client: BillingClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog)
        donate()
    }

    fun donate() = runBlocking {
        try {
            client = makeClient { purchases -> thanksDialog(purchases) }
            val skus = connect().await()
            skusDialog(skus)
        } catch (e: IOException) {
            errorDialog()
        }
    }

    fun errorDialog() {
        alert {
            title = getString(app_name)
            message = "Impossible de se connecter aux services Google Play"
            positiveButton("réessayer") { donate() }
        }.show()
    }

    fun thanksDialog(purchases: List<Purchase>) {
        purchases.forEach {
            client.consumeAsync(it.purchaseToken, fun(_, _) {})
        }

        alert {
            title = getString(app_name)
            message = "Merci pour ton don <3"
        }.show()
    }

    fun makeClient(listener: (List<Purchase>) -> Unit): BillingClient {
        return BillingClient.newBuilder(this)
            .setListener { res, purchases -> if (res == OK) listener(purchases!!) }
            .build()
    }

    fun connect(): Deferred<List<SkuDetails>> = client.run {
        return Promise { resolve, reject ->
            startConnection(StateListener(resolve, reject))
        }
    }

    fun purchase(client: BillingClient, sku: SkuDetails) {
        val flow = BillingFlowParams.newBuilder().setSkuDetails(sku).build()
        client.launchBillingFlow(this, flow)
    }

    fun skusDialog(skus: List<SkuDetails>) {
        if (!client.isReady) return

        fun String.dropLastWord(): String {
            return split(" ").dropLast(1).joinToString(" ")
        }

        fun row(sku: SkuDetails): LinearLayout {
            val linearLayout = linearLayout {
                verticalPadding = dip(16)
                textView {
                    val title = sku.title.dropLastWord()
                    text = Html.fromHtml("<b>$title</b> (${sku.price})")
                    textSize = 20f
                    setOnClickListener { purchase(client, sku) }
                }
            }
            return linearLayout
        }

        verticalLayout {
            horizontalPadding = dip(16)
            skus.forEach { sku -> row(sku) }
        }
    }
}

fun <T> Promise(block: ((T) -> Unit, (Throwable) -> Unit) -> Unit): Deferred<T> {
    val deferred = CompletableDeferred<T>()

    fun resolve(value: T) {
        deferred.complete(value)
    }

    fun reject(error: Throwable) {
        deferred.completeExceptionally(error)
    }

    block(::resolve, ::reject)
    return deferred
}

fun BillingClient.StateListener(
    resolve: (List<SkuDetails>) -> Unit,
    reject: (IOException) -> Unit
) = object : BillingClientStateListener {
    override fun onBillingServiceDisconnected() = reject(IOException("Disconected"))
    override fun onBillingSetupFinished(@BillingResponse res: Int) {
        if (res != OK) return
        val params = SkuDetailsParams.newBuilder().setSkusList(skuList).setType(INAPP).build()
        querySkuDetailsAsync(params, fun(res, skus) {
            if (res != OK || skus == null) return
            skus.sortBy { it.priceAmountMicros }
            resolve(skus)
        })
    }
}