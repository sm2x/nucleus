package com.domatix.yevbes.nucleus.sales.fragments

import android.app.Activity
import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.text.SpannableStringBuilder
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.domatix.yevbes.nucleus.*
import com.domatix.yevbes.nucleus.core.Odoo
import com.domatix.yevbes.nucleus.databinding.FragmentAddSaleBinding
import com.domatix.yevbes.nucleus.products.entities.ProductProduct
import com.domatix.yevbes.nucleus.sales.activities.OrderLineListActivity
import com.domatix.yevbes.nucleus.sales.activities.OrderLineManagerActivity
import com.domatix.yevbes.nucleus.sales.activities.PricelistListActivity
import com.domatix.yevbes.nucleus.sales.adapters.AddOrderLinesAdapter
import com.domatix.yevbes.nucleus.sales.customer.CustomerListActivity
import com.domatix.yevbes.nucleus.sales.entities.ProductPricelist
import com.domatix.yevbes.nucleus.sales.entities.SaleOrderLine
import com.domatix.yevbes.nucleus.sales.interfaces.LongShortOrderItemClick
import com.domatix.yevbes.nucleus.utils.MyProgressDialog
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.reflect.TypeToken
import io.reactivex.disposables.CompositeDisposable
import org.joda.time.DateTimeZone
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

/**
 * A simple [Fragment] subclass.
 *
 */
class AddSaleFragment : Fragment() {

    companion object {
        const val ADD_SALE_FRAG_TAG: String = "AddSaleFragment"
        const val SELECTED_LIST = "SaleOrderLineSelectedList"
        const val CUSTOMER_ID = "CustomerId"
        const val PRICELIST_ID = "PRICELIST_ID"
        const val INVOICE_ID = "InvoiceId"
        const val SHIPPING_ID = "ShippingId"
        const val CUSTOMER_NAME = "CustomerName"
        const val COMPANY_NAME = "CompanyName"
        const val CUSTOMER_PRICELIST = "CUSTOMER_PRICELIST"
        const val REQUEST_CODE = 1
        const val CUSTOMER_REQUEST_CODE = 2
        const val INVOICE_REQUEST_CODE = 3
        const val SHIPPING_REQUEST_CODE = 4
        const val SALES_MANAGER_REQUEST_CODE = 5
        const val PRICELIST_REQUEST_CODE = 6


        @JvmStatic
        fun newInstance() =
                AddSaleFragment().apply {
                    arguments = Bundle().apply {

                    }
                }
    }

    private var idCustomer: Int? = null
    private var idPriceList: Int? = null
    private var partnerInvoiceId: Int? = null
    private var partnerShippingId: Int? = null
    private lateinit var drawerToggle: ActionBarDrawerToggle
    lateinit var activity: MainActivity private set
    lateinit var binding: FragmentAddSaleBinding private set
    lateinit var compositeDisposable: CompositeDisposable private set
    lateinit var selectedOrderLines: ArrayList<SaleOrderLine> private set
    lateinit var progressDialog: ProgressDialog private set
    lateinit var myProgressDialog: MyProgressDialog private set
    private val productPricelistType = object : TypeToken<ProductPricelist>() {}.type


    private val adapterSelectedListOrderLineDataAdapter: AddOrderLinesAdapter by lazy {
        AddOrderLinesAdapter(this, arrayListOf(), object : LongShortOrderItemClick {
            override fun onItemClick(view: View) {
                val position = binding.rvSelectedOrderLines.getChildAdapterPosition(view)

                val intent = Intent(activity, OrderLineManagerActivity::class.java)

                val bundle = Bundle()
                val aux = ArrayList<SaleOrderLine>()
                aux.addAll(selectedOrderLines)


                bundle.putString(OrderLineManagerActivity.SELECTED_LIST, gson.toJson(aux))
                bundle.putInt(OrderLineManagerActivity.SELECTED_LIST_POSITION, position)
                intent.putExtras(bundle)

                startActivityForResult(intent, AddSaleFragment.SALES_MANAGER_REQUEST_CODE)
                Timber.v("ITEM_CLICKED_$position")
            }

            override fun onLongItemClick(view: View) {
                val items = arrayOf(getString(R.string.remove_sale_order_line))

                val builder = AlertDialog.Builder(ContextThemeWrapper(activity, R.style.AlertDialog))
                builder.setTitle(getString(R.string.select_action))
                // Set items form alert dialog
                builder.setItems(items) { _, which ->
                    when (which) {
                        0 -> {
                            val position = binding.rvSelectedOrderLines.getChildAdapterPosition(view)
                            if (::selectedOrderLines.isInitialized) {
                                adapterSelectedListOrderLineDataAdapter.removeItem(position)
                                selectedOrderLines.removeAt(position)
                            }
                        }
                    }
                }
                val dialog = builder.create()
                dialog.show()
            }
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        setHasOptionsMenu(true)
        compositeDisposable = CompositeDisposable()
        // Inflate the layout for this fragment
        binding = FragmentAddSaleBinding.inflate(inflater, container, false)
        progressDialog = ProgressDialog(context)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        activity = getActivity() as MainActivity

        // get Shared Preferences value
        val sharedPref = activity.getSharedPreferences(getString(R.string.preference_fle_key_res_config_settings), Context.MODE_PRIVATE)
                ?: return
        val saleNote = sharedPref.getString(getString(R.string.saved_sale_note), "")
        if (!saleNote.isNullOrEmpty())
            binding.termsConditions.text = SpannableStringBuilder(saleNote)

        //activity.binding.tb.title = getString(R.string.action_sales)
        activity.setTitle(R.string.action_sales)
        activity.binding.abl.visibility = View.GONE
        activity.binding.nsv.visibility = View.GONE

        activity.setSupportActionBar(binding.tb)
        val actionBar = activity.supportActionBar
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true)
            actionBar.setDisplayHomeAsUpEnabled(true)
        }

        drawerToggle = ActionBarDrawerToggle(activity, activity.binding.dl,
                binding.tb, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        activity.binding.dl.addDrawerListener(drawerToggle)
        drawerToggle.syncState()

        activity.binding.dl.addDrawerListener(drawerToggle)

        binding.buttonAddOrderSalesLine.setOnClickListener {
            if (idCustomer != null) {
                val intent = Intent(activity, OrderLineListActivity::class.java)
                startActivityForResult(intent, REQUEST_CODE)
            }else{
                Toast.makeText(activity,getString(R.string.choose_customer),Toast.LENGTH_SHORT).show()
            }
        }

        binding.saleDate.setOnClickListener {
            getDate()
        }

        binding.customer.setOnClickListener {
            val intent = Intent(activity, CustomerListActivity::class.java)
            intent.putExtra(CustomerListActivity.TYPE, 0)
            startActivityForResult(intent, CUSTOMER_REQUEST_CODE)
            //startActivity(intent)
        }

        binding.billing.setOnClickListener {
            val intent = Intent(activity, CustomerListActivity::class.java)
            intent.putExtra(CustomerListActivity.TYPE, 1)
            startActivityForResult(intent, INVOICE_REQUEST_CODE)
        }

        binding.delivery.setOnClickListener {
            val intent = Intent(activity, CustomerListActivity::class.java)
            intent.putExtra(CustomerListActivity.TYPE, 1)
            startActivityForResult(intent, SHIPPING_REQUEST_CODE)
        }

        binding.pricelist.setOnClickListener {
            val intent = Intent(activity, PricelistListActivity::class.java)
            startActivityForResult(intent, PRICELIST_REQUEST_CODE)
        }

        binding.favIconConfirm.setOnClickListener {
            if (idCustomer != null) {
                addOrder(partnerInvoiceId!!, partnerShippingId!!, idCustomer!!, context!!, binding.termsConditions.text.toString())
            } else {
                Toast.makeText(context, getString(R.string.fail_order_no_customer), Toast.LENGTH_SHORT).show()
            }
        }

        val mLayoutManager = LinearLayoutManager(context)
        binding.rvSelectedOrderLines.layoutManager = mLayoutManager
        binding.rvSelectedOrderLines.itemAnimator = DefaultItemAnimator()
        binding.rvSelectedOrderLines.adapter = adapterSelectedListOrderLineDataAdapter
        adapterSelectedListOrderLineDataAdapter.setupScrollListener(binding.rvSelectedOrderLines)

        showActualDate()
    }

    private fun showActualDate() {
        val c = Calendar.getInstance().time
        val df = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.FRANCE)
        val formattedDate = df.format(c)
        binding.saleDate.text = formattedDate
    }

    private fun showActualTime() {
        val c = Calendar.getInstance().time
        val df = SimpleDateFormat("HH:mm:ss", Locale.FRANCE)
        val formattedTime = df.format(c)
        binding.saleDate.text = "${binding.saleDate.text} $formattedTime"
    }

    private fun addOrderLines(orderId: Long, selectedItems: ArrayList<SaleOrderLine>, index: Int) {
        var count: Int
        val item = selectedItems[index]
        Odoo.create(model = "sale.order.line", values = mapOf(
                "order_id" to orderId,
                "product_id" to item.productId.asJsonArray.get(0).asLong,
                "product_uom_qty" to item.qty,
                "price_unit" to item.priceUnit,
                "name" to item.name,
                "discount" to item.discount

        )) {
            onSubscribe { disposable ->
                compositeDisposable.add(disposable)
            }

            onNext { response ->
                if (response.isSuccessful) {
                    val create = response.body()!!
                    if (create.isSuccessful) {
                        // Devuelve ID del pedido creado
                        val result = create.result
                    } else {
                        // Odoo specific error
                        Timber.w("create() failed with ${create.errorMessage}")
                    }
                } else {
                    Timber.w("request failed with ${response.code()}:${response.message()}")
                }
            }

            onError { error ->
                error.printStackTrace()
            }

            onComplete {
                count = index + 1

                when {
                    count <= selectedItems.size - 1 -> {
                        addOrderLines(orderId, selectedItems, count)
                        myProgressDialog.setProgressToProgressDialog(myProgressDialog.getProgressDialog() + 70 / selectedItems.size)
                    }
                    count == selectedItems.size -> {
                        myProgressDialog.setProgressToProgressDialog(100)
                        myProgressDialog.dismissProgressDialog()
                        Toast.makeText(context, context!!.getString(R.string.order_added), Toast.LENGTH_SHORT).show()
                        fragmentManager?.popBackStack()
                    }
                }
            }
        }

    }

    private fun addOrder(partnerInvoiceId: Int, partnerShippingId: Int, idCustomer: Int, context: Context, conditions: String) {
        val date = fromStringToDate(binding.saleDate.text.toString(), "dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val convertedDate = convertDateToSpecificTimeZone(date, TimeZone.getDefault(), DateTimeZone.forTimeZone(TimeZone.getTimeZone("GMT+00:00")))
        myProgressDialog = MyProgressDialog(progressDialog, getString(R.string.adding_sale_order_dialog_title), getString(R.string.please_wait_dialog_message))
        myProgressDialog.setProgressToProgressDialog(25)

        var result: Long? = null
        Odoo.create(model = "sale.order", values = mapOf(
                "partner_id" to idCustomer,
                "partner_invoice_id" to partnerInvoiceId,
                "partner_shipping_id" to partnerShippingId,
                "note" to conditions,
                "date_order" to convertedDate
        )) {
            onSubscribe { disposable ->
                compositeDisposable.add(disposable)
            }

            onNext { response ->
                if (response.isSuccessful) {
                    val create = response.body()!!
                    if (create.isSuccessful) {
                        // Devuelve ID del pedido creado
                        result = create.result
                        //res = result

                    } else {
                        // Odoo specific error
                        Timber.w("create() failed with ${create.errorMessage}")
                    }
                } else {
                    Timber.w("request failed with ${response.code()}:${response.message()}")
                }
            }

            onError { error ->
                error.printStackTrace()
            }

            onComplete {
                if (!::selectedOrderLines.isInitialized || selectedOrderLines.isEmpty()) {
                    myProgressDialog.setProgressToProgressDialog(100)
                    myProgressDialog.dismissProgressDialog()
                    Toast.makeText(context, context.getString(R.string.order_added), Toast.LENGTH_SHORT).show()
                    fragmentManager?.popBackStack()
                } else {
                    myProgressDialog.setProgressToProgressDialog(30)
                    addOrderLines(result!!, selectedOrderLines, 0)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        activity.binding.nv.menu.findItem(R.id.nav_sales).isChecked = true
    }

    private fun getUnitPrice(tarifId: Int, productId: Int, quantity: Float, listener: GetTarifaInterface) {
        Odoo.callKw(model = "product.pricelist", method = "price_get", args = listOf(tarifId,
                productId,quantity))
        {
            onSubscribe { disposable ->
                compositeDisposable.add(disposable)
            }

            onNext { response ->
                if (response.isSuccessful) {
                    val callKw = response.body()!!
                    if (callKw.isSuccessful) {
                        val result = callKw.result
                        val tarifa = result.asJsonObject[tarifId.toString()].asFloat
                        listener.getTarifa(tarifa)
                    } else {
                        // Odoo specific error
                        Timber.w("callkw() failed with ${callKw.errorMessage}")
                    }
                } else {
                    Timber.w("request failed with ${response.code()}:${response.message()}")
                }
            }

            onError { error ->
                error.printStackTrace()
            }

            onComplete {

            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        when (requestCode) {
            REQUEST_CODE -> {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        val selectedItemsJSONString = data?.getStringExtra(SELECTED_LIST)
                        val selectedItemsGson = Gson()

                        val auxSaleOrderLineList = ArrayList<SaleOrderLine>()
                        val auxProductProductList: ArrayList<ProductProduct>

                        if (!::selectedOrderLines.isInitialized)
                            selectedOrderLines = ArrayList()

                        auxProductProductList = selectedItemsGson.fromJson(selectedItemsJSONString, object : TypeToken<java.util.ArrayList<ProductProduct>>() {
                        }.type)

                        for (index in 0 until auxProductProductList.size){
                            getUnitPrice(idPriceList!!,auxProductProductList[index].id,auxProductProductList[index].quantity, object : GetTarifaInterface {
                                override fun getTarifa(tarifa: Float) {
                                    val jsonArray = JsonArray()
                                    jsonArray.add(auxProductProductList[index].id)
                                    jsonArray.add(auxProductProductList[index].name)

                                    /*auxSaleOrderLineList.add(SaleOrderLine(
                                            0,
                                            auxProductProductList[index].name,
                                            jsonArray,
                                            auxProductProductList[index].quantity,
                                            0f,
                                            auxProductProductList[index].lstPrice,
                                            0f
                                            ,
                                            auxProductProductList[index].lstPrice * auxProductProductList[index].quantity,
                                            auxProductProductList[index].taxesId
                                    ))*/

                                    val saleOrderLine = SaleOrderLine(
                                            0,
                                            auxProductProductList[index].name,
                                            jsonArray,
                                            auxProductProductList[index].quantity,
                                            0f,
                                            auxProductProductList[index].lstPrice,
                                            tarifa
                                            ,
                                            auxProductProductList[index].lstPrice * auxProductProductList[index].quantity,
                                            auxProductProductList[index].taxesId
                                    )
                                    selectedOrderLines.add(saleOrderLine)
//                                    selectedOrderLines.addAll(auxSaleOrderLineList)
                                    adapterSelectedListOrderLineDataAdapter.addRowItem(saleOrderLine)
//                                    adapterSelectedListOrderLineDataAdapter.addRowItems(auxSaleOrderLineList)
//                                    adapterSelectedListOrderLineDataAdapter.notifyDataSetChanged()
                                }
                            })
                        }
                    }

                    Activity.RESULT_CANCELED -> {

                    }
                }
            }

            PRICELIST_REQUEST_CODE -> {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        val item = gson.fromJson<ProductPricelist>(data?.getStringExtra(PRICELIST_ID), productPricelistType)

                        if (idPriceList != item.id) {
                            idPriceList = item.id
                            binding.pricelist.text = item.displayName
                        }
                    }
                }
            }

            CUSTOMER_REQUEST_CODE -> {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        idCustomer = data?.getIntExtra(CUSTOMER_ID, 0)
                        partnerInvoiceId = idCustomer
                        partnerShippingId = idCustomer

                        val productlist = data?.getStringExtra(CUSTOMER_PRICELIST)?.toJsonElement()!!
                        if (!productlist.isJsonPrimitive) {
                            binding.pricelist.text = productlist.asJsonArray[1].asString
                            idPriceList = productlist.asJsonArray[0].asInt
                        }
//                        binding.billing.text = data?.getStringExtra(CUSTOMER_BILLING_ADDRESS)
//                        binding.delivery.text = data?.getStringExtra(CUSTOMER_DELIVERY_ADDRESS)
                        if (data?.getStringExtra(COMPANY_NAME) != "") {
                            binding.customer.text = "${data?.getStringExtra(COMPANY_NAME)}, ${data?.getStringExtra(CUSTOMER_NAME)}"
                            binding.billing.text = "${data?.getStringExtra(COMPANY_NAME)}, ${data?.getStringExtra(CUSTOMER_NAME)}"
                            binding.delivery.text = "${data?.getStringExtra(COMPANY_NAME)}, ${data?.getStringExtra(CUSTOMER_NAME)}"
                        } else {
                            binding.customer.text = data?.getStringExtra(CUSTOMER_NAME)
                            binding.billing.text = data?.getStringExtra(CUSTOMER_NAME)
                            binding.delivery.text = data?.getStringExtra(CUSTOMER_NAME)
                        }
                    }
                }
            }

            INVOICE_REQUEST_CODE -> {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        partnerInvoiceId = data?.getIntExtra(CUSTOMER_ID, 0)
//                        binding.billing.text = data?.getStringExtra(CUSTOMER_BILLING_ADDRESS)
//                        binding.delivery.text = data?.getStringExtra(CUSTOMER_DELIVERY_ADDRESS)
                        if (data?.getStringExtra(COMPANY_NAME) != "") {
                            binding.billing.text = "${data?.getStringExtra(COMPANY_NAME)}, ${data?.getStringExtra(CUSTOMER_NAME)}"
                        } else {
                            binding.billing.text = data?.getStringExtra(CUSTOMER_NAME)
                        }
                    }
                }
            }

            SHIPPING_REQUEST_CODE -> {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        partnerShippingId = data?.getIntExtra(CUSTOMER_ID, 0)
//                        binding.billing.text = data?.getStringExtra(CUSTOMER_BILLING_ADDRESS)
//                        binding.delivery.text = data?.getStringExtra(CUSTOMER_DELIVERY_ADDRESS)
                        if (data?.getStringExtra(COMPANY_NAME) != "") {
                            binding.delivery.text = "${data?.getStringExtra(COMPANY_NAME)}, ${data?.getStringExtra(CUSTOMER_NAME)}"
                        } else {
                            binding.delivery.text = data?.getStringExtra(CUSTOMER_NAME)
                        }
                    }
                }
            }

            AddSaleFragment.SALES_MANAGER_REQUEST_CODE -> {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        val selectedJSONString = data?.getStringExtra(AddSaleFragment.SELECTED_LIST)!!
                        val map: HashMap<Int, SaleOrderLine>

                        val type = object : TypeToken<Map<Int, SaleOrderLine>>() {

                        }.type
                        map = Gson().fromJson(selectedJSONString, type)


                        for (i in map.keys) {
                            selectedOrderLines[i] = map[i]!!
                        }

                        /*selectedSaleOrderLineItems.addAll(selectedItemsGson.fromJson(selectedItemsJSONString, object : TypeToken<ArrayList<SaleOrderLine>>() {
                        }.type))*/
                        adapterSelectedListOrderLineDataAdapter.clear()
                        adapterSelectedListOrderLineDataAdapter.addRowItems(selectedOrderLines)
                        adapterSelectedListOrderLineDataAdapter.notifyDataSetChanged()

                        //selectedItemsJSONString = selectedItemsGson.toJson(selectedSaleOrderLineItems)
                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private interface GetTarifaInterface{
        fun getTarifa(tarifa: Float)
    }

    override fun onDestroy() {
        super.onDestroy()
        activity.binding.nv.menu.findItem(R.id.nav_sales).isChecked = false
        compositeDisposable.dispose()
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        if (::drawerToggle.isInitialized) {
            drawerToggle.onConfigurationChanged(newConfig)
        }
    }

    private fun getDate() {
        val zero = "0"
        val slash = "/"
        val c = Calendar.getInstance()

        val monthCal = c.get(Calendar.MONTH)
        val dayCal = c.get(Calendar.DAY_OF_MONTH)
        val yearCal = c.get(Calendar.YEAR)

        var actualMonth: Int
        var formattedDay: String = ""
        var formattedMonth: String = ""
        var formattedYear: Int = 2019

        val getDate = DatePickerDialog(context, DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            /*          actualMonth = month + 1
                     formattedYear = year
                      formattedDay = if (dayOfMonth < 10) zero + dayOfMonth.toString() else dayOfMonth.toString()
                      formattedMonth = if (actualMonth < 10) zero + actualMonth.toString() else actualMonth.toString()*/

//            binding.saleDate.text = formattedDay + slash + formattedMonth + slash + year
        },
                yearCal, monthCal, dayCal)
        getDate.setButton(DialogInterface.BUTTON_POSITIVE, "OK") { _, p1 ->
            if (p1 == DialogInterface.BUTTON_POSITIVE) {
                val datePicker = getDate.datePicker

                actualMonth = datePicker.month + 1
                formattedYear = datePicker.year
                formattedDay = if (datePicker.dayOfMonth < 10) zero + datePicker.dayOfMonth.toString() else datePicker.dayOfMonth.toString()
                formattedMonth = if (actualMonth < 10) zero + actualMonth.toString() else actualMonth.toString()
                binding.saleDate.text = formattedDay + slash + formattedMonth + slash + formattedYear
                getTime()
            }
        }
        getDate.show()
    }

    private fun getTime() {
        val mCurrentTime = Calendar.getInstance()
        val hour = mCurrentTime.get(Calendar.HOUR_OF_DAY)
        val minute = mCurrentTime.get(Calendar.MINUTE)
        val second = mCurrentTime.get(Calendar.SECOND)

        val timePickerDialog = TimePickerDialog(context, TimePickerDialog.OnTimeSetListener { p0, p1, p2 ->
            binding.saleDate.text = "${binding.saleDate.text} ${checkDigit(p1)}:${checkDigit(p2)}:${checkDigit(second)}"
        }, hour, minute, true)

        timePickerDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.cancel)) { _, p1 ->
            if (p1 == DialogInterface.BUTTON_NEGATIVE) {
                showActualTime()
            }
        }

        timePickerDialog.setTitle(getString(R.string.select_time))
        timePickerDialog.show()
    }

    private fun checkDigit(number: Int): String {
        return if (number <= 9) {
            "0$number"
        } else {
            number.toString()
        }
    }
}