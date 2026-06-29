# Sections

Use sections when a Nav3 KMP app has top-level areas such as tabs, rails, or
main product sections.

Laydr remembers one stack per section and validates which section owns each
generated destination. Your app still owns labels, icons, order, visibility,
badges, and chrome.

The useful shift is that section membership comes from generated route refs,
while the visible tab, rail, or product section UI stays ordinary app code.

## When To Use Sections

Use sections when:

- top-level destinations should keep independent stack history
- Back should pop within the selected top-level area
- cross-section navigation should optionally return to the source stack
- chrome such as tabs or rails selects between app areas

Use a single stack instead when the app has one linear navigation surface.

## Section Data Is App Data

Attach the visual or product data your shell needs:

```kotlin
private data class TabSpec(val label: String)

val sections = LaydrNavRoutes.rememberSections(
    sectionSpecs = listOf(
        LaydrNavRoutes.Contacts.section(TabSpec("Contacts")),
        LaydrNavRoutes.Profile.section(TabSpec("Profile")),
    ),
    notFoundContent = { notFound -> NotFound(notFound) },
)
```

The `TabSpec` type is yours. It can include labels, icons, order, badges, or
visibility flags. Laydr stores it; it does not interpret it.

## Render App-Owned Chrome

Your shell reads the selected section and renders chrome in ordinary Compose:

```kotlin
BottomTabBar(
    sections = sections.sectionSet.items,
    selectedSection = sections.selectedSection,
    onSelect = sections::select,
)
```

`sections.select(section)` changes the selected section without mutating the
section stacks.

## Keep Policy Out Of Route Layouts

A section scaffold is shell behavior. It controls the area around the selected
Nav3 stack. Do not force tabs, rails, or selected-section state into a route
`Layout.kt`.

It is fine for a route subtree to own a presentational scaffold component, such
as `routes/main/MainTabScaffold.kt`, when the shell passes state and callbacks
into it. The shell still owns `rememberSections`, selected state, app Back, and
`NavDisplay`.

## Route-Facing Navigation

Route entries should usually receive a narrow capability, not the whole section
runtime. The route-facing capability is `LaydrNavSectionsNavigator`.

```kotlin
internal class ProfileNavigation(
    private val navigator: LaydrNavSectionsNavigator,
) {
    fun openContact(id: String) {
        navigator.push(
            LaydrRoutes.Contacts.ById.destination(
                id = LaydrRoutes.Contacts.ById.id(id),
            ),
        )
    }
}
```

This keeps route code bounded to declared sections and keeps owner-facing
operations, such as external targets, in the shell.

## Dynamic Section Roots

If a section root has dynamic parameters, provide the root destination
explicitly when declaring the section.

Use this for product areas such as workspace tabs where the section root is
not parameterless.

For example, a workspace shell can create one section per open workspace:

```kotlin
LaydrNavRoutes.Workspaces.ById.section(
    rootDestination = LaydrRoutes.Workspaces.ById.destination(
        id = LaydrRoutes.Workspaces.ById.id("alpha"),
    ),
    sectionData = WorkspaceTab(label = "Alpha"),
)
```

The generated route ref still defines the section. The explicit destination
only supplies the parameter value for that section root.
