package com.guddiz.app.ui.navigation

object Routes {
    const val DASHBOARD = "dashboard"
    const val PRODUCTS = "products"
    const val PRODUCTS_LOW_STOCK = "products_low_stock"
    const val PRODUCTS_EXPIRING = "products_expiring"
    const val RECORDS = "records"
    const val REPORTS = "reports"
    const val BACKUP = "backup"
    const val RECYCLE_BIN = "recycle_bin"

    const val PRODUCT_FORM = "product_form/{productId}"
    fun productForm(productId: Long = 0L) = "product_form/$productId"
    const val PRODUCT_DETAIL = "product_detail/{productId}"
    fun productDetail(productId: Long) = "product_detail/$productId"

    const val SALES = "sales"
    const val SALE_FORM = "sale_form/{saleId}"
    fun saleForm(saleId: Long = 0L) = "sale_form/$saleId"

    const val PURCHASES = "purchases"
    const val PURCHASE_FORM = "purchase_form/{purchaseId}"
    fun purchaseForm(purchaseId: Long = 0L) = "purchase_form/$purchaseId"

    const val EXPENSES = "expenses"
    const val EXPENSE_FORM = "expense_form/{expenseId}"
    fun expenseForm(expenseId: Long = 0L) = "expense_form/$expenseId"

    const val PR = "pr_marketing"
    const val PR_FORM = "pr_form/{prId}"
    fun prForm(prId: Long = 0L) = "pr_form/$prId"

    const val GIFTS = "gifts"
    const val GIFT_FORM = "gift_form/{giftId}"
    fun giftForm(giftId: Long = 0L) = "gift_form/$giftId"

    const val FOC = "foc"
    const val FOC_FORM = "foc_form/{focId}"
    fun focForm(focId: Long = 0L) = "foc_form/$focId"

    const val ARG_PRODUCT_ID = "productId"
    const val ARG_SALE_ID = "saleId"
    const val ARG_PURCHASE_ID = "purchaseId"
    const val ARG_EXPENSE_ID = "expenseId"
    const val ARG_PR_ID = "prId"
    const val ARG_GIFT_ID = "giftId"
    const val ARG_FOC_ID = "focId"
}
