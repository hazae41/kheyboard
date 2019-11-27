package fr.rhaz.kheyboard

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import com.android.billingclient.api.BillingClient.BillingResponse
import com.android.billingclient.api.BillingClient.BillingResponse.OK
import com.android.billingclient.api.BillingClient.SkuType.INAPP
import com.bumptech.glide.Glide
import fr.rhaz.kheyboard.utils.deferred
import fr.rhaz.kheyboard.utils.dropLastWord
import fr.rhaz.kheyboard.utils.job
import kotlinx.android.synthetic.main.billing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.IOException

val skuList = listOf("sku_small", "sku_medium", "sku_large", "sku_xlarge", "sku_xxlarge")

class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val title: TextView = itemView.findViewById(R.id.title)
    val price: TextView = itemView.findViewById(R.id.price)
}

class Billing : AppCompatActivity() {
    lateinit var client: BillingClient
    val dataSource = emptyDataSourceTyped<SkuDetails>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.billing)

        client = BillingClient { thanksDialog() }

        recyclerView.setup {
            withDataSource(dataSource)
            withItem<SkuDetails, ItemViewHolder>(R.layout.billing_item) {
                onBind(::ItemViewHolder) { index, item ->
                    title.text = item.title.dropLastWord()
                    price.text = item.price
                }
                onClick { index -> purchase(item) }
            }
        }

        Glide.with(this).load("https://image.noelshack.com/fichiers/2016/47/1480099705-ristasargent.gif").into(sticker)

        button.setOnClickListener {
            val uri = Uri.parse("market://details?id=$packageName")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            startActivity(intent)
        }

        retrieve()
    }

    fun purchase(sku: SkuDetails) {
        val flow = BillingFlowParams.newBuilder().setSkuDetails(sku).build()
        client.launchBillingFlow(this, flow)
    }

    fun retrieve() = GlobalScope.launch(Dispatchers.Main) {
        try {
            client.connect().join()
            val skus = client.query().await()
            dataSource.set(skus)
        } catch (e: IOException) {
            errorDialog()
        }
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
            message(text = "Merci pour ton don <3")
        }
    }
}

fun Context.BillingClient(listener: (List<Purchase>) -> Unit) =
        BillingClient.newBuilder(this)
                .setListener { res, purchases -> if (res == OK) listener(purchases!!) }
                .build()


fun BillingClient.connect() = job { resolve, reject ->
    startConnection(object : BillingClientStateListener {
        override fun onBillingServiceDisconnected() = reject(IOException("Disconected"))
        override fun onBillingSetupFinished(@BillingResponse res: Int) {
            if (res != OK) reject(IOException("Error"))
            else resolve()
        }
    })
}

fun BillingClient.query() = deferred<List<SkuDetails>> { resolve, reject ->
    val params = SkuDetailsParams.newBuilder().setSkusList(skuList).setType(INAPP).build()
    querySkuDetailsAsync(params, fun(res, skus) {
        if (res != OK) return reject(IOException("Error"))
        skus.sortBy { it.priceAmountMicros }
        resolve(skus)
    })
}