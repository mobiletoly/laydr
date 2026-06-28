# Nav3 KMP Shopping Example

This is a runnable KMP shopping example for `laydr-nav3-kmp`.

It demonstrates a deeper Laydr Nav3 KMP app shape:

- App code navigates with generated `LaydrRoutes.*.destination(...)` values.
- Each screen route owns local `Route.kt` and `Screen.kt` files.
- `cart/checkout` also owns a local `Layout.kt` for the checkout flow shell.
- `cart/checkout/review` hosts a private review/confirm workflow from
  app-owned Compose code.
- The shell uses a mixed root `LaydrNavStack` over an app-owned `NavBackStack`
  for sectioned shopping plus root entries such as account sign-in.
- The section surface uses generated `LaydrNavRoutes.rememberSections` for
  repeated adapter setup.
- Checkout launches reusable routes for typed address, payment, and sign-in
  results.
- The example uses Koin as app composition infrastructure.
- Nav3 renders the selected stack with app-owned `NavDisplay`.
- Compact screens use bottom navigation, while wide screens use a side rail.

The route tree is:

```text
routes/
  account/sign_in/
  shop/
    categories/by_id/
    products/by_product_id/reviews/
    products/by_product_id/seller/
  search/results/
  cart/checkout/shipping/
  cart/checkout/payment/
  cart/checkout/review/
  cart/checkout/confirmation/
  orders/by_order_id/tracking/
  profile/addresses/
  profile/addresses/select/
  profile/payment_methods/
  profile/payment_methods/select/
  profile/settings/
```

Grouping routes such as `shop/categories`, `shop/products`, and
`cart/checkout` are explicit screen routes so the filesystem remains the full
navigation map. `cart/checkout` is also a layout route that wraps the checkout
flow descendants. Route-local `Route.kt` files bind nearby `::Screen` and
`::Layout` functions; shared UI helpers stay in the app package. The checkout
review route owns its workflow nodes, renderer, workflow creation, output
collection, and workflow host in app-owned `Screen.kt`.

The shell creates a small Koin module with `ShoppingContext` as an example
facade. It carries the fake `ShoppingStore` and section navigator only.
Route-local `Screen.kt` and `Layout.kt` files use `koinInject()` where they
need that facade, so dependency lookup stays near route entry content instead
of becoming a provider cascade around `NavDisplay`. Koin is not a Laydr
requirement, and generated code plus route-local `Route.kt` files stay
DI-framework-neutral.

The app shell declares sections for Shop, Search, Cart, Orders, and Profile.
Laydr validates generated destinations, chooses the owning section, preserves
one stack per section, and keeps labels and visual navigation app-owned.

Checkout demonstrates two result paths:

- shipping launches `profile/addresses/select` with
  `LaydrNavSectionsNavigator.pushForResult` and stores an
  `AddressSelectionResult`
- payment launches `profile/payment_methods/select` the same way and stores a
  `PaymentMethodSelectionResult`
- guest checkout launches root `account/sign_in` with
  `requireLaydrNavStackNavigator().pushForResult`, a `SignInRoutePayload`,
  and a `SignInRouteResult`

Sign-in and selector screens read their payload or result sinks locally and
then leave through the provided root or section navigator. Address and payment
selectors are cross-section result launches, so their explicit section Back
returns to the checkout caller stack after the selector completes or cancels
its result sink. The sign-in launch also attaches app-owned
`LaydrNavEntryMetadata` through a typed shopping metadata key; Laydr transports
that metadata through `NavEntry.metadata`, while the shopping shell decides how
to present it.

The shared module opts into `laydr-nav3-kmp-adaptive` and declares two
adaptive list/detail scenes:

- `LaydrRoutes.Shop` to `LaydrRoutes.Shop.Products.ByProductId`
- `LaydrRoutes.Orders` to `LaydrRoutes.Orders.ByOrderId`

Direct detail navigation can initialize the list plus detail stack on wide
screens, while compact screens behave like normal pushed destinations.

The fake shopping state is local to the example. It has product catalog data,
cart lines, checkout flow state, orders, addresses, and payment methods. It
does not add persistence, networking, ViewModels, or framework APIs.

The iOS launcher lives in `iosApp/iosApp.xcodeproj`. Open that project in
Xcode and run the `Nav3KmpShoppingIosApp` scheme on an iOS simulator. The
Xcode target calls Gradle's `embedAndSignAppleFrameworkForXcode` task to build
and embed the shared KMP framework before compiling Swift.

Useful checks:

```sh
./gradlew :examples:nav3-kmp-shopping:shared:checkLaydrRoutes
./gradlew :examples:nav3-kmp-shopping:shared:compileKotlinDesktop
./gradlew :examples:nav3-kmp-shopping:shared:compileAndroidMain
./gradlew :examples:nav3-kmp-shopping:shared:compileKotlinIosSimulatorArm64
./gradlew :examples:nav3-kmp-shopping:desktopApp:compileKotlin
./gradlew :examples:nav3-kmp-shopping:desktopApp:run --dry-run
./gradlew :examples:nav3-kmp-shopping:androidApp:compileDebugKotlin
xcodebuild -project examples/nav3-kmp-shopping/iosApp/iosApp.xcodeproj -scheme Nav3KmpShoppingIosApp -configuration Debug -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' build
```
