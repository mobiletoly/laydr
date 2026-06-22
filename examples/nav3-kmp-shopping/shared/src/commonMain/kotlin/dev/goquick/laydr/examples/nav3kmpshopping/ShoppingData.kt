package dev.goquick.laydr.examples.nav3kmpshopping

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.goquick.laydr.nav3.kmp.laydrNavEntryMetadataKey

internal data class Category(
    val id: String,
    val name: String,
    val summary: String,
)

internal data class Product(
    val id: String,
    val categoryId: String,
    val name: String,
    val seller: String,
    val priceCents: Int,
    val summary: String,
    val rating: String,
    val stockLabel: String,
)

internal data class CartLine(
    val productId: String,
    val quantity: Int,
)

internal data class CartItem(
    val product: Product,
    val quantity: Int,
)

internal data class Order(
    val id: String,
    val status: String,
    val totalCents: Int,
    val productIds: List<String>,
    val tracking: String,
)

internal data class Address(
    val id: String,
    val label: String,
    val body: String,
)

internal data class PaymentMethod(
    val id: String,
    val label: String,
    val detail: String,
)

internal data class ShoppingCustomer(
    val id: String,
    val displayName: String,
)

internal data class SignInRoutePayload(
    val initialEmail: String?,
    val reason: String,
)

internal data class SignInRouteResult(
    val customerId: String,
    val displayName: String,
)

internal data class AddressSelectionResult(
    val addressId: String,
)

internal data class PaymentMethodSelectionResult(
    val paymentMethodId: String,
)

internal enum class ShoppingEntryPresentation {
    Fullscreen,
    Overlay,
}

internal val ShoppingEntryPresentationKey =
    laydrNavEntryMetadataKey<ShoppingEntryPresentation>("shopping:presentation")

internal class ShoppingStore(
    val categories: List<Category>,
    val products: List<Product>,
    addresses: List<Address>,
    paymentMethods: List<PaymentMethod>,
    initialOrders: List<Order>,
) {
    private val cartState = mutableStateListOf<CartLine>()
    private val orderState = mutableStateListOf<Order>().apply {
        addAll(initialOrders)
    }
    private val addressState = mutableStateListOf<Address>().apply {
        addAll(addresses)
    }
    private val paymentMethodState = mutableStateListOf<PaymentMethod>().apply {
        addAll(paymentMethods)
    }

    private var nextOrderNumber = 1003

    var latestOrderId: String? by mutableStateOf(orderState.firstOrNull()?.id)
        private set

    var currentCustomer: ShoppingCustomer? by mutableStateOf(null)
        private set

    val isGuest: Boolean
        get() = currentCustomer == null

    var selectedCheckoutAddressId: String? by mutableStateOf(addresses.firstOrNull()?.id)
        private set

    var selectedCheckoutPaymentMethodId: String? by mutableStateOf(paymentMethods.firstOrNull()?.id)
        private set

    var checkoutFeedbackText: String? by mutableStateOf(null)
        private set

    val cartLines: List<CartLine>
        get() = cartState

    val orders: List<Order>
        get() = orderState

    val customerAddresses: List<Address>
        get() = addressState

    val customerPaymentMethods: List<PaymentMethod>
        get() = paymentMethodState

    fun featuredProducts(): List<Product> =
        products.take(4)

    fun findProduct(id: String): Product? =
        products.firstOrNull { product -> product.id == id }

    fun findCategory(id: String): Category? =
        categories.firstOrNull { category -> category.id == id }

    fun findOrder(id: String): Order? =
        orderState.firstOrNull { order -> order.id == id }

    fun findAddress(id: String?): Address? =
        addressState.firstOrNull { address -> address.id == id }

    fun findPaymentMethod(id: String?): PaymentMethod? =
        paymentMethodState.firstOrNull { method -> method.id == id }

    fun selectedCheckoutAddress(): Address? =
        findAddress(selectedCheckoutAddressId)

    fun selectedCheckoutPaymentMethod(): PaymentMethod? =
        findPaymentMethod(selectedCheckoutPaymentMethodId)

    fun productsForCategory(categoryId: String): List<Product> =
        products.filter { product -> product.categoryId == categoryId }

    fun addToCart(productId: String) {
        val index = cartState.indexOfFirst { line -> line.productId == productId }
        if (index >= 0) {
            val current = cartState[index]
            cartState[index] = current.copy(quantity = current.quantity + 1)
        } else {
            cartState.add(CartLine(productId = productId, quantity = 1))
        }
    }

    fun cartItems(): List<CartItem> =
        cartState.mapNotNull { line ->
            findProduct(line.productId)?.let { product ->
                CartItem(product = product, quantity = line.quantity)
            }
        }

    fun cartTotalCents(): Int =
        cartItems().sumOf { item -> item.product.priceCents * item.quantity }

    fun selectCheckoutAddress(addressId: String) {
        selectedCheckoutAddressId = addressId
        val address = findAddress(addressId)
        checkoutFeedbackText = "Shipping address set to ${address?.label ?: addressId}."
    }

    fun selectCheckoutPaymentMethod(paymentMethodId: String) {
        selectedCheckoutPaymentMethodId = paymentMethodId
        val method = findPaymentMethod(paymentMethodId)
        checkoutFeedbackText = "Payment method set to ${method?.label ?: paymentMethodId}."
    }

    fun applySignInResult(result: SignInRouteResult) {
        currentCustomer = ShoppingCustomer(
            id = result.customerId,
            displayName = result.displayName,
        )
        checkoutFeedbackText = "Signed in as ${result.displayName}."
    }

    fun showCheckoutFeedback(message: String) {
        checkoutFeedbackText = message
    }

    fun placeOrder(): String {
        val items = cartItems()
        val orderId = "ord-${nextOrderNumber++}"
        val order = Order(
            id = orderId,
            status = "Preparing",
            totalCents = cartTotalCents(),
            productIds = items.map { item -> item.product.id },
            tracking = "Warehouse assigned. Carrier pickup is scheduled.",
        )
        orderState.add(0, order)
        latestOrderId = orderId
        cartState.clear()
        return orderId
    }
}

internal fun seedShoppingStore(): ShoppingStore =
    ShoppingStore(
        categories = listOf(
            Category(
                id = "packs",
                name = "Packs",
                summary = "Commuter bags, travel packs, and small carry kits.",
            ),
            Category(
                id = "outerwear",
                name = "Outerwear",
                summary = "Light layers for changing city weather.",
            ),
            Category(
                id = "home-office",
                name = "Home office",
                summary = "Desk tools, lighting, and everyday organizers.",
            ),
        ),
        products = listOf(
            Product(
                id = "day-pack",
                categoryId = "packs",
                name = "Day Pack",
                seller = "Northline Goods",
                priceCents = 12800,
                summary = "A structured everyday pack with laptop storage and side access.",
                rating = "4.8",
                stockLabel = "In stock",
            ),
            Product(
                id = "field-sling",
                categoryId = "packs",
                name = "Field Sling",
                seller = "Northline Goods",
                priceCents = 6400,
                summary = "A compact crossbody sling for phone, keys, and small camera gear.",
                rating = "4.6",
                stockLabel = "Low stock",
            ),
            Product(
                id = "packable-jacket",
                categoryId = "outerwear",
                name = "Packable Jacket",
                seller = "Harbor Supply",
                priceCents = 9800,
                summary = "A water-resistant shell that packs into its own pocket.",
                rating = "4.7",
                stockLabel = "In stock",
            ),
            Product(
                id = "desk-lamp",
                categoryId = "home-office",
                name = "Angle Desk Lamp",
                seller = "Studio North",
                priceCents = 8600,
                summary = "A dimmable task lamp with a small footprint and warm LEDs.",
                rating = "4.5",
                stockLabel = "In stock",
            ),
            Product(
                id = "cable-kit",
                categoryId = "home-office",
                name = "Cable Kit",
                seller = "Studio North",
                priceCents = 3200,
                summary = "A zip case with labeled straps for chargers and adapters.",
                rating = "4.4",
                stockLabel = "In stock",
            ),
        ),
        addresses = listOf(
            Address(
                id = "home",
                label = "Home",
                body = "411 Market Street, San Francisco, CA",
            ),
            Address(
                id = "studio",
                label = "Studio",
                body = "28 King Street, Seattle, WA",
            ),
        ),
        paymentMethods = listOf(
            PaymentMethod(
                id = "card-primary",
                label = "Primary card",
                detail = "Visa ending in 4242",
            ),
            PaymentMethod(
                id = "card-backup",
                label = "Backup card",
                detail = "Mastercard ending in 8100",
            ),
        ),
        initialOrders = listOf(
            Order(
                id = "ord-1001",
                status = "Delivered",
                totalCents = 12800,
                productIds = listOf("day-pack"),
                tracking = "Delivered to Home on Monday.",
            ),
            Order(
                id = "ord-1002",
                status = "In transit",
                totalCents = 13000,
                productIds = listOf("field-sling", "cable-kit"),
                tracking = "Arriving tomorrow. Last scan: regional sorting center.",
            ),
        ),
    )

internal fun formatPrice(cents: Int): String {
    val whole = cents / 100
    val fraction = (cents % 100).toString().padStart(length = 2, padChar = '0')
    return "${'$'}$whole.$fraction"
}
