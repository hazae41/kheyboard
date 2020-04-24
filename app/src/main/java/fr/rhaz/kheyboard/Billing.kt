package fr.rhaz.kheyboard

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.callbacks.onDismiss
import com.afollestad.recyclical.datasource.emptyDataSourceTyped
import com.afollestad.recyclical.setup
import com.afollestad.recyclical.withItem
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponseCode.OK
import com.android.billingclient.api.BillingClient.SkuType.INAPP
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.billing.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

val skuList = listOf("sku_small", "sku_medium")

fun String.dropLastWord(): String {
    return split(" ").dropLast(1).joinToString(" ")
}

class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val title: TextView = itemView.findViewById(R.id.title)
    val price: TextView = itemView.findViewById(R.id.price)
    val description: TextView = itemView.findViewById(R.id.description)
}

class Billing : AppCompatActivity() {
    lateinit var client: BillingClient
    val dataSource = emptyDataSourceTyped<SkuDetails>()
    val settings get() = File(getExternalFilesDir(null), "settings.json")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.billing)

        client = BillingClient { purchases ->
            GlobalScope.launch(IO) {
                purchases.forEach { client.ack(it) }
                withContext(Main) { checkPremium() }
            }
        }

        recyclerView.setup {
            withDataSource(dataSource)
            withItem<SkuDetails, ItemViewHolder>(R.layout.billing_item) {
                onBind(::ItemViewHolder) { index, item ->
                    title.text = item.title.dropLastWord()
                    price.text = item.price
                    description.text = item.description
                }
                onClick { index -> purchase(item) }
            }
        }

        Glide.with(this)
            .load("https://image.noelshack.com/fichiers/2016/47/1480099705-ristasargent.gif")
            .into(sticker)

        retrieve()
    }

    fun purchase(sku: SkuDetails) {
        val flow = BillingFlowParams.newBuilder().setSkuDetails(sku).build()
        client.launchBillingFlow(this, flow)
    }

    fun retrieve() = GlobalScope.launch(Main) {
        try {
            client.connect().join()
            val skus = client.all().await()
            dataSource.set(skus)
            checkPremium()
        } catch (e: IOException) {
            errorDialog()
        }
    }

    fun checkPremium() {
        if (!client.isPremium()) return
        json(settings) { put("premium", true) }
        thanksDialog()
    }

    fun errorDialog() {
        MaterialDialog(this).show {
            title(text = "Erreur")
            message(text = "Impossible de se connecter aux services Google Play")
            positiveButton(text = "Réessayer") { retrieve() }
            onDismiss { finish() }
        }
    }

    fun thanksDialog() {
        MaterialDialog(this).show {
            title(text = "Merci")
            message(text = "Tu fais maintenant partie de l'élite")
        }
    }
}

fun Context.BillingClient(listener: (List<Purchase>) -> Unit) =
    BillingClient.newBuilder(this)
        .setListener { res, purchases -> if (res.responseCode == OK) listener(purchases!!) }
        .enablePendingPurchases()
        .build()


fun BillingClient.connect() = job { resolve, reject ->
    startConnection(object : BillingClientStateListener {
        override fun onBillingServiceDisconnected() = reject(IOException("Disconected"))
        override fun onBillingSetupFinished(res: BillingResult) {
            if (res.responseCode != OK) reject(IOException("Error"))
            else resolve()
        }
    })
}

suspend fun BillingClient.ack(purchase: Purchase) {
    val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken)
    acknowledgePurchase(params.build())
}

fun BillingClient.isPremium(): Boolean {
    val res = queryPurchases(INAPP)
    println(res.billingResult.debugMessage)
    if (res.responseCode != OK) return false;
    else return res.purchasesList.any { it.isAcknowledged }
}

fun BillingClient.all() = deferred<List<SkuDetails>> { resolve, reject ->
    val params = SkuDetailsParams.newBuilder().setSkusList(skuList).setType(INAPP).build()
    querySkuDetailsAsync(params, fun(res, skus) {
        if (res.responseCode != OK) return reject(IOException("Error"))
        skus.sortBy { it.priceAmountMicros }
        resolve(skus)
    })
}