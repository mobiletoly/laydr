package dev.goquick.laydr.examples.nav3kmpshopping

import androidx.compose.runtime.Composable

internal fun orderProductNames(
    store: ShoppingStore,
    order: Order,
): String {
    val names = order.productIds.mapNotNull { productId ->
        store.findProduct(productId)?.name
    }
    return names.ifEmpty { listOf("No products recorded") }.joinToString()
}

@Composable
internal fun ProductCard(
    product: Product,
    onClick: () -> Unit,
) {
    ItemCard(
        title = product.name,
        subtitle = product.summary,
        meta = "${formatPrice(product.priceCents)} | ${product.seller} | Rating ${product.rating}",
        onClick = onClick,
    )
}
