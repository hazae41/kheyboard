package fr.rhaz.kheyboard

import android.app.Activity
import android.content.Context
import android.graphics.Typeface
import android.text.Html
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponse
import com.android.billingclient.api.BillingClient.BillingResponse.OK
import com.android.billingclient.api.BillingClient.SkuType.*
import fr.rhaz.kheyboard.R.string.app_name
import org.jetbrains.anko.*

fun Activity.billingClient(then: (List<Purchase>) -> Unit)
= BillingClient.newBuilder(this)
    .setListener { res, purchases -> if(res == OK) then(purchases!!) }
    .build()

fun Activity.startBilling(){
    lateinit var client: BillingClient
    client = billingClient { purchases ->
        alert{
            purchases.forEach { client.consumeAsync(it.purchaseToken){ res, token -> } }
            title = getString(app_name)
            message = "Merci pour ton don <3"
            positiveButton("fermer"){}
            show()
        }
    }
    client.startConnection(billingListener(client))
}

val skuList = listOf("sku_small", "sku_medium", "sku_large", "sku_xlarge", "sku_xxlarge")

fun Activity.billingListener(client: BillingClient) = object: BillingClientStateListener {
    val listener = this
    override fun onBillingSetupFinished(@BillingResponse res: Int) {
        if(res == OK){
            val params = SkuDetailsParams.newBuilder().setSkusList(skuList).setType(INAPP).build()
            client.querySkuDetailsAsync(params){ res, skus ->
                if(res == OK && skus != null){
                    skus.sortBy { it.priceAmountMicros }
                    show(client, skus)
                }
            }
        }
    }
    override fun onBillingServiceDisconnected() {
        alert {
            title = getString(app_name)
            message = "Impossible de se connecter aux services Google Play"
            positiveButton("réessayer"){client.startConnection(listener)}
        }
    }
}

fun Activity.show(client: BillingClient, skus: List<SkuDetails>){
    alert{
        customView {
            verticalLayout{
                padding = dip(16)
                skus.forEach { sku ->
                    linearLayout {
                        verticalPadding = dip(16)
                        textView{
                            val title = sku.title.split(" ").dropLast(1).joinToString(" ")
                            text = Html.fromHtml("<b>$title</b> (${sku.price})")
                            textSize = 20f
                            setOnClickListener {
                                val flow = BillingFlowParams.newBuilder().setSkuDetails(sku).build()
                                client.launchBillingFlow(act, flow)
                            }
                        }
                    }
                }
            }
        }
        positiveButton("fermer"){}
        show()
    }
}