package routes.cart.checkout

internal val Route = LaydrRouteDef.screenAndLayout(name = "Checkout") {
    screen(content = ::Screen)
    layout(content = ::Layout)
}
