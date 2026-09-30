package com.guddiz.app.ui.products

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guddiz.app.core.money.Money
import com.guddiz.app.data.local.entity.ProductCategory
import com.guddiz.app.data.local.entity.ProductEntity
import com.guddiz.app.data.repository.ProductRepository
import com.guddiz.app.ui.navigation.Routes
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProductFormState(
    val id: Long = 0L,
    val name: String = "",
    val sku: String = "",
    val productCode: String = "",
    val category: ProductCategory = ProductCategory.SKINCARE,
    val brand: String = "",
    val supplier: String = "",
    val purchasePrice: String = "",
    val prs: String = "",
    val sellingPrice: String = "",
    val currentQuantity: String = "0",
    val minStockLevel: String = "0",
    val batchNumber: String = "",
    val notes: String = "",
    val photoPath: String? = null,
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errors: Map<String, String> = emptyMap()
)

sealed interface ProductFormEvent {
    data class Message(val text: String, val isError: Boolean) : ProductFormEvent
    data object Saved : ProductFormEvent
}

class ProductFormViewModel(
    private val repository: ProductRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(ProductFormState())
    val state: StateFlow<ProductFormState> = _state.asStateFlow()

    private val _events = Channel<ProductFormEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        val id = savedStateHandle.get<String>(Routes.ARG_PRODUCT_ID)?.toLongOrNull() ?: 0L
        if (id > 0L) loadProduct(id)
    }

    private fun loadProduct(id: Long) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, id = id, isEditing = true) }
            val p = repository.getProduct(id)
            if (p == null) {
                _events.send(ProductFormEvent.Message("This product no longer exists.", true))
                return@launch
            }
            _state.update {
                ProductFormState(
                    id = p.id,
                    name = p.name,
                    sku = p.sku.orEmpty(),
                    productCode = p.productCode.orEmpty(),
                    category = ProductCategory.fromStorage(p.category),
                    brand = p.brand.orEmpty(),
                    supplier = p.supplier.orEmpty(),
                    purchasePrice = Money(p.purchasePrice).toEditText(),
                    prs = Money(p.prs).toEditText(),
                    sellingPrice = Money(p.sellingPrice).toEditText(),
                    currentQuantity = p.currentQuantity.toString(),
                    minStockLevel = p.minStockLevel.toString(),
                    batchNumber = p.batchNumber.orEmpty(),
                    notes = p.notes.orEmpty(),
                    photoPath = p.photoPath,
                    isEditing = true,
                    isLoading = false
                )
            }
        }
    }

    fun onNameChange(v: String) = update { it.copy(name = v) }
    fun onSkuChange(v: String) = update { it.copy(sku = v.uppercase()) }
    fun onProductCodeChange(v: String) = update { it.copy(productCode = v) }
    fun onCategoryChange(v: ProductCategory) = update { it.copy(category = v) }
    fun onBrandChange(v: String) = update { it.copy(brand = v) }
    fun onSupplierChange(v: String) = update { it.copy(supplier = v) }
    fun onPurchasePriceChange(v: String) = update { it.copy(purchasePrice = v) }
    fun onPrsChange(v: String) = update { it.copy(prs = v) }
    fun onSellingPriceChange(v: String) = update { it.copy(sellingPrice = v) }
    fun onQuantityChange(v: String) = update { it.copy(currentQuantity = v.filter(Char::isDigit)) }
    fun onMinStockChange(v: String) = update { it.copy(minStockLevel = v.filter(Char::isDigit)) }
    fun onBatchChange(v: String) = update { it.copy(batchNumber = v) }
    fun onNotesChange(v: String) = update { it.copy(notes = v) }
    fun onPhotoChange(path: String?) = update { it.copy(photoPath = path) }

    private fun update(block: (ProductFormState) -> ProductFormState) {
        _state.update { block(it).copy(errors = emptyMap()) }
    }

    fun save() {
        val s = _state.value
        val errors = validate(s)
        if (errors.isNotEmpty()) { _state.update { it.copy(errors = errors) }; return }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }

            if (s.sku.isNotBlank() && repository.isSkuTaken(s.sku, s.id)) {
                _state.update {
                    it.copy(isSaving = false,
                        errors = mapOf("sku" to "This SKU is already used by another product."))
                }
                return@launch
            }

            val entity = ProductEntity(
                id = s.id,
                name = s.name.trim(),
                sku = s.sku.trim().ifBlank { null },
                productCode = s.productCode.trim().ifBlank { null },
                category = s.category.name,
                brand = s.brand.trim().ifBlank { null },
                supplier = s.supplier.trim().ifBlank { null },
                purchasePrice = Money.parse(s.purchasePrice)?.paisa ?: 0L,
                prs = Money.parse(s.prs)?.paisa ?: 0L,
                sellingPrice = Money.parse(s.sellingPrice)?.paisa ?: 0L,
                currentQuantity = s.currentQuantity.toIntOrNull() ?: 0,
                minStockLevel = s.minStockLevel.toIntOrNull() ?: 0,
                batchNumber = s.batchNumber.trim().ifBlank { null },
                notes = s.notes.trim().ifBlank { null },
                photoPath = s.photoPath
            )

            try {
                repository.save(entity)
                _events.send(ProductFormEvent.Saved)
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false) }
                _events.send(ProductFormEvent.Message("Could not save the product. Please try again.", true))
            }
        }
    }

    private fun validate(s: ProductFormState): Map<String, String> = buildMap {
        if (s.name.isBlank()) put("name", "Please enter a product name.")
        if (s.purchasePrice.isNotBlank() && Money.parse(s.purchasePrice) == null)
            put("purchasePrice", "Enter a valid amount, e.g. 1250.00")
        if (s.prs.isNotBlank() && Money.parse(s.prs) == null)
            put("prs", "Enter a valid amount, e.g. 1499")
        if (s.sellingPrice.isNotBlank() && Money.parse(s.sellingPrice) == null)
            put("sellingPrice", "Enter a valid amount, e.g. 1499")

        val purchase = Money.parse(s.purchasePrice) ?: Money.ZERO
        val selling = Money.parse(s.sellingPrice) ?: Money.ZERO
        if (selling.isPositive && purchase.isPositive && selling < purchase)
            put("sellingPrice", "Selling price is below purchase price.")
    }
}
