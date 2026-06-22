package routes.contacts

internal val Route = LaydrRouteDef.screenAndLayout {
    screenWithLayoutValues { route ->
        ContactsScreen(route = route)
    }
    layout { context, content ->
        ContactsLayout(
            context = context,
            content = content,
        )
    }
}
