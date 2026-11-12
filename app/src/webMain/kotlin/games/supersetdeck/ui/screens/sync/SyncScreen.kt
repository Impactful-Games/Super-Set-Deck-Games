package games.supersetdeck.ui.screens.sync

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import games.supersetdeck.data.Backup
import games.supersetdeck.data.Backup.BackupList
import games.supersetdeck.data.BackupStorage
import games.supersetdeck.data.Games
import games.supersetdeck.data.GoogleDriveStorage
import games.supersetdeck.data.Lists
import games.supersetdeck.data.LocalFileStorage
import games.supersetdeck.data.Settings
import games.supersetdeck.data.Sync
import games.supersetdeck.data.SyncDirection
import games.supersetdeck.data.SyncPlan
import games.supersetdeck.data.SyncRow
import games.supersetdeck.data.UserList
import games.supersetdeck.i18n.LocalStrings
import games.supersetdeck.i18n.Strings
import games.supersetdeck.platform.formatDateTime
import games.supersetdeck.platform.openTextFile
import games.supersetdeck.platform.userMessage
import games.supersetdeck.ui.components.ButtonKind
import games.supersetdeck.ui.components.ConfirmDialog
import games.supersetdeck.ui.components.ErrorCard
import games.supersetdeck.ui.components.LocalWindowLayout
import games.supersetdeck.ui.components.SsdButton
import games.supersetdeck.ui.components.SsdIconButton
import games.supersetdeck.ui.components.dashedBorder
import games.supersetdeck.ui.components.handCursor
import games.supersetdeck.ui.components.rememberPageScroll
import games.supersetdeck.ui.icons.Icons
import games.supersetdeck.ui.screens.BackButton
import games.supersetdeck.ui.screens.ScreenColumn
import games.supersetdeck.ui.screens.ScreenHeader
import games.supersetdeck.ui.screens.ScreenTitle
import games.supersetdeck.ui.theme.Expressive
import games.supersetdeck.ui.theme.Pill
import games.supersetdeck.ui.theme.ShapeBadge
import games.supersetdeck.ui.theme.SsdTheme
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

private sealed interface Step {
    data object Choose : Step
    data object Working : Step
    data class Configure(
        val storage: BackupStorage,
        val app: Backup,
        val backup: Backup,
        val plan: SyncPlan,
        val directions: Map<String, SyncDirection>,
    ) : Step
    data object Done : Step
    data class Failed(val message: String) : Step
}

/**
 * Backup / restore of the lists, list by list and in both directions (spec § 8): choose a storage,
 * load and compare, configure, apply. Only an explicit user action leaves it.
 */
@Composable
fun SyncScreen(onExit: () -> Unit) {
    val strings = LocalStrings.current
    val layout = LocalWindowLayout.current
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf<Step>(Step.Choose) }
    var confirmDriveDelete by remember { mutableStateOf(false) }

    fun load(storage: BackupStorage) {
        step = Step.Working
        scope.launch {
            step = try {
                val backup = Backup.decode(storage.read())
                val app = Backup.of(Lists.data ?: error(strings.unknownError))
                if (app.sameAs(backup)) Step.Done
                else {
                    val plan = Sync.plan(app, backup)
                    Step.Configure(storage, app, backup, plan, plan.rows.associate { it.id to it.default })
                }
            } catch (error: Throwable) {
                Step.Failed(error.userMessage())
            }
        }
    }

    fun synchronize(configure: Step.Configure) {
        step = Step.Working
        scope.launch {
            step = try {
                val result = Sync.apply(configure.app, configure.backup, configure.plan, configure.directions)
                val written = result.backup.sameAs(configure.backup) || configure.storage.write(result.backup.encode())
                if (!written) {
                    // The user cancelled the choice of the destination file: back to the configuration.
                    configure
                } else {
                    result.appChanges.forEach { (id, list) ->
                        Lists.restore(id, list.content?.name, list.content?.games, list.update.toDouble())
                    }
                    Step.Done
                }
            } catch (error: Throwable) {
                Step.Failed(error.userMessage())
            }
        }
    }

    val page = rememberPageScroll()
    ScreenColumn(page, top = if (layout.wide) 32.dp else 12.dp, spacing = if (layout.wide) 28.dp else 14.dp) {
        ScreenHeader {
            BackButton(strings.backToLists, onExit, size = if (layout.wide) 48.dp else 44.dp)
            ScreenTitle(
                strings.backupRestore,
                style = if (layout.wide) SsdTheme.type.displayMedium.copy(fontSize = 52.sp, lineHeight = 56.sp, fontWeight = FontWeight.Bold) else SsdTheme.type.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
            )
        }

        when (val s = step) {
            Step.Choose -> Choose(
                onDrive = {
                    scope.launch(start = CoroutineStart.UNDISPATCHED) {
                        try {
                            load(GoogleDriveStorage.connect())
                        } catch (error: Throwable) {
                            step = Step.Failed(error.userMessage())
                        }
                    }
                },
                onNewFile = { load(LocalFileStorage(null, strings.backupFileType)) },
                onOpenFile = {
                    // Undispatched: the file picker opens during the click, as browsers require.
                    scope.launch(start = CoroutineStart.UNDISPATCHED) {
                        openTextFile(".ssdbak")?.let { load(LocalFileStorage(it, strings.backupFileType)) }
                    }
                },
            )
            Step.Working -> Working()
            is Step.Configure -> Configure(
                s,
                onDirection = { id, direction -> step = s.copy(directions = s.directions + (id to direction)) },
                onDeleteDrive = { confirmDriveDelete = true },
                onCancel = onExit,
                onSynchronize = { synchronize(s) },
            )
            Step.Done -> Done(onExit)
            is Step.Failed -> {
                ErrorCard(s.message)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    SsdButton(strings.cancel, onExit, kind = ButtonKind.Text)
                }
            }
        }
    }

    val configure = step as? Step.Configure
    val drive = configure?.storage as? GoogleDriveStorage
    if (confirmDriveDelete && drive != null) {
        ConfirmDialog(
            icon = Icons.CloudOff,
            title = strings.deleteDriveBackup,
            text = drive.lastBackup?.let { strings.driveLastBackup(formatDateTime(it, Settings.lang.code)) } ?: "",
            confirmLabel = strings.delete,
            textAlign = TextAlign.Center,
            onConfirm = {
                confirmDriveDelete = false
                step = Step.Working
                scope.launch {
                    try {
                        drive.delete()
                        load(drive)
                    } catch (error: Throwable) {
                        step = Step.Failed(error.userMessage())
                    }
                }
            },
            onDismiss = { confirmDriveDelete = false },
        )
    }
}

@Composable
private fun Choose(onDrive: () -> Unit, onNewFile: () -> Unit, onOpenFile: () -> Unit) {
    val strings = LocalStrings.current
    val c = SsdTheme.colors
    val wide = LocalWindowLayout.current.wide
    LaunchedEffect(Unit) { GoogleDriveStorage.preload() }
    val drive = @Composable { modifier: Modifier ->
        Surface(
            onClick = onDrive,
            modifier = modifier.handCursor(),
            shape = RoundedCornerShape(32.dp),
            color = c.primaryContainer,
            contentColor = c.onPrimaryContainer,
        ) {
            Column(Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                StorageIcon(Expressive.Cookie9, Icons.Cloud)
                Box(Modifier.weight(1f, fill = false))
                Text(strings.syncWithGoogleDrive, style = SsdTheme.type.headlineSmall.copy(lineHeight = 30.sp))
            }
        }
    }
    val local = @Composable { modifier: Modifier ->
        Surface(modifier, shape = RoundedCornerShape(32.dp), color = c.secondaryContainer, contentColor = c.onSecondaryContainer) {
            Column(Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                StorageIcon(Expressive.Cookie12, Icons.File)
                Text(strings.syncWithLocalFiles, style = SsdTheme.type.headlineSmall.copy(lineHeight = 30.sp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SsdButton(strings.createNewFile, onNewFile, icon = Icons.Plus)
                    SsdButton(strings.openExistingFile, onOpenFile, kind = ButtonKind.Outlined, icon = Icons.Folder)
                }
            }
        }
    }
    if (wide) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            drive(Modifier.weight(1f).fillMaxHeight())
            local(Modifier.weight(1f).fillMaxHeight())
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            drive(Modifier.fillMaxWidth())
            local(Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun StorageIcon(shape: Expressive, icon: ImageVector) {
    ShapeBadge(shape, SsdTheme.colors.surface, 88.dp) {
        Icon(icon, null, Modifier.size(40.dp), tint = SsdTheme.colors.primaryText)
    }
}

@Composable
private fun Working() {
    val strings = LocalStrings.current
    val c = SsdTheme.colors
    Column(Modifier.fillMaxWidth().semantics { contentDescription = strings.loadingBackup }, verticalArrangement = Arrangement.spacedBy(28.dp)) {
        LinearWavyProgressIndicator(Modifier.fillMaxWidth().height(12.dp), color = c.primary, trackColor = c.primaryContainer)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(3) { Box(Modifier.fillMaxWidth().height(64.dp).alpha(if (it == 2) 0.6f else 1f).clip(RoundedCornerShape(22.dp)).background(c.surfaceContainerLow)) }
        }
    }
}

@Composable
private fun Done(onExit: () -> Unit) {
    val strings = LocalStrings.current
    val c = SsdTheme.colors
    Column(
        Modifier.fillMaxWidth().padding(vertical = 48.dp).semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        ShapeBadge(Expressive.Sunny, c.primary, 150.dp, rotation = -8f) {
            Icon(Icons.Check, null, Modifier.size(64.dp), tint = c.onPrimary)
        }
        Text(strings.upToDate, style = SsdTheme.type.headlineLarge.copy(fontSize = 30.sp, fontWeight = FontWeight.ExtraBold), textAlign = TextAlign.Center)
        SsdIconButton(Icons.ArrowBack, strings.backToLists, onExit, size = 52.dp, iconSize = 24.dp)
    }
}

@Composable
private fun Configure(
    s: Step.Configure,
    onDirection: (String, SyncDirection) -> Unit,
    onDeleteDrive: () -> Unit,
    onCancel: () -> Unit,
    onSynchronize: () -> Unit,
) {
    val strings = LocalStrings.current
    val c = SsdTheme.colors
    val wide = LocalWindowLayout.current.wide
    val lang = Settings.lang.code
    val drive = s.storage as? GoogleDriveStorage
    if (drive != null) {
        Surface(shape = RoundedCornerShape(if (wide) 28.dp else 22.dp), color = c.secondaryContainer, contentColor = c.onSecondaryContainer) {
            Row(Modifier.padding(start = 22.dp, end = 12.dp, top = 14.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (wide) {
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(c.surface), contentAlignment = Alignment.Center) {
                        Icon(Icons.Cloud, null, Modifier.size(26.dp), tint = c.primaryText)
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (wide) Text(strings.syncWithGoogleDrive, style = SsdTheme.type.labelLarge.copy(fontSize = 13.sp))
                    Text(
                        drive.lastBackup?.let { strings.driveLastBackup(formatDateTime(it, lang)) } ?: strings.driveNewFile,
                        style = SsdTheme.type.bodyLarge.copy(fontSize = if (wide) 17.sp else 14.sp, lineHeight = if (wide) 24.sp else 19.sp),
                    )
                }
                if (drive.lastBackup != null) {
                    if (wide) SsdButton(strings.delete, onDeleteDrive, kind = ButtonKind.DangerText, icon = Icons.Delete, height = 44.dp)
                    else SsdIconButton(Icons.Delete, strings.deleteDriveBackup, onDeleteDrive, container = Color.Transparent, content = c.error, size = 44.dp, iconSize = 20.dp)
                }
            }
        }
    }

    val middle = if (wide) 176.dp else 48.dp
    val gap = if (wide) 16.dp else 8.dp
    if (s.plan.rows.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = if (wide) 12.dp else 8.dp), horizontalArrangement = Arrangement.spacedBy(gap)) {
                Text(strings.columnApplication.uppercase(), Modifier.weight(1f), style = SsdTheme.type.overline.copy(fontSize = if (wide) 13.sp else 12.sp), color = c.onSurfaceVariant)
                Box(Modifier.width(middle))
                Text(strings.columnBackup.uppercase(), Modifier.weight(1f), style = SsdTheme.type.overline.copy(fontSize = if (wide) 13.sp else 12.sp), color = c.onSurfaceVariant)
            }
            s.plan.rows.forEach { row ->
                SyncRowView(row, s.directions[row.id] ?: row.default, strings, lang, middle, gap) { onDirection(row.id, it) }
            }
        }
    }

    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End), verticalAlignment = Alignment.CenterVertically) {
        SsdButton(strings.cancel, onCancel, kind = ButtonKind.Text, height = 56.dp, textStyle = SsdTheme.type.labelLarge.copy(fontSize = 16.sp))
        SsdButton(
            strings.synchronize, onSynchronize, icon = Icons.Sync, height = if (wide) 64.dp else 56.dp, elevation = 4.dp,
            textStyle = SsdTheme.type.labelLarge.copy(fontSize = if (wide) 18.sp else 17.sp),
            modifier = if (wide) Modifier else Modifier.weight(1f),
        )
    }
}

private enum class SideKind { Plain, New, Delete, Replace, Tombstone, Absent }

private class Line(val text: String, val old: String? = null)

private class SideView(val kind: SideKind, val name: String, val lines: List<Line>)

private fun Strings.listName(id: String, content: Backup.Content?): String =
    if (id == UserList.FAVOURITES) favourites else content?.name.orEmpty()

/**
 * What a side shows: on the destination side, a preview of the result (creation, deletion, or the
 * information that changes); on the other side, or when paused, the current state.
 */
private fun Strings.side(id: String, side: BackupList?, source: BackupList?, isDestination: Boolean, lang: String): SideView {
    fun date(list: BackupList) = formatDateTime(list.update.toDouble(), lang)
    // As in My Lists, games missing from the catalogue are not counted.
    fun count(content: Backup.Content) = games(if (Games.all.isEmpty()) content.games.size else content.games.count { Games[it] != null })
    val content = side?.content
    val sourceContent = source?.content
    return when {
        isDestination && sourceContent != null && content == null ->
            SideView(SideKind.New, listName(id, sourceContent), listOf(Line(date(source)), Line(count(sourceContent))))
        isDestination && source != null && sourceContent == null && content != null ->
            SideView(SideKind.Delete, listName(id, content), listOf(Line(date(side)), Line(count(content))))
        isDestination && sourceContent != null && content != null -> {
            fun changed(old: String, new: String) = Line(new, old.takeIf { it != new })
            val oldName = listName(id, content)
            val newName = listName(id, sourceContent)
            SideView(
                SideKind.Replace,
                newName,
                listOfNotNull(
                    Line(newName, oldName).takeIf { oldName != newName },
                    changed(date(side), date(source)),
                    changed(count(content), count(sourceContent)),
                ),
            )
        }
        content != null -> SideView(SideKind.Plain, listName(id, content), listOf(Line(date(side)), Line(count(content))))
        side != null -> SideView(SideKind.Tombstone, deletedList, listOf(Line(date(side))))
        else -> SideView(SideKind.Absent, absentList, emptyList())
    }
}

@Composable
private fun SyncRowView(
    row: SyncRow,
    direction: SyncDirection,
    strings: Strings,
    lang: String,
    middle: androidx.compose.ui.unit.Dp,
    gap: androidx.compose.ui.unit.Dp,
    onDirection: (SyncDirection) -> Unit,
) {
    val c = SsdTheme.colors
    val wide = LocalWindowLayout.current.wide
    val app = strings.side(row.id, row.app, row.backup, direction == SyncDirection.ToApp, lang)
    val backup = strings.side(row.id, row.backup, row.app, direction == SyncDirection.ToBackup, lang)
    val label = strings.listName(row.id, row.app?.content ?: row.backup?.content)
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min).clip(RoundedCornerShape(if (wide) 28.dp else 24.dp)).background(c.surfaceContainerLow).padding(if (wide) 12.dp else 8.dp),
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SideBox(app, Modifier.weight(1f))
        Directions(row, direction, label, strings, Modifier.width(middle), vertical = !wide, onDirection = onDirection)
        SideBox(backup, Modifier.weight(1f))
    }
}

@Composable
private fun SideBox(view: SideView, modifier: Modifier) {
    val c = SsdTheme.colors
    val strings = LocalStrings.current
    val wide = LocalWindowLayout.current.wide
    val shape = RoundedCornerShape(if (wide) 20.dp else 16.dp)
    val boxModifier = when (view.kind) {
        SideKind.New -> Modifier.background(c.surface, shape).dashedBorder(2.dp, c.primary, if (wide) 20.dp else 16.dp)
        SideKind.Delete -> Modifier.background(c.errorContainer, shape).border(2.dp, c.error, shape)
        SideKind.Replace -> Modifier.background(c.surface, shape).border(2.dp, c.primary, shape)
        SideKind.Absent -> Modifier.dashedBorder(1.dp, c.outlineVariant, if (wide) 20.dp else 16.dp)
        else -> Modifier.background(c.surface, shape).border(1.dp, c.outlineVariant, shape)
    }
    Column(
        modifier.fillMaxHeight().heightIn(min = if (wide) 76.dp else 84.dp).then(boxModifier).padding(horizontal = if (wide) 16.dp else 12.dp, vertical = if (wide) 14.dp else 10.dp),
        verticalArrangement = Arrangement.spacedBy(if (wide) 4.dp else 2.dp, Alignment.CenterVertically),
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp), itemVerticalAlignment = Alignment.CenterVertically) {
            Text(
                view.name,
                style = SsdTheme.type.labelLarge.copy(
                    fontSize = if (wide) 17.sp else 15.sp,
                    fontStyle = if (view.kind == SideKind.Tombstone || view.kind == SideKind.Absent) FontStyle.Italic else FontStyle.Normal,
                    fontWeight = if (view.kind == SideKind.Tombstone || view.kind == SideKind.Absent) FontWeight.SemiBold else FontWeight.Bold,
                    textDecoration = if (view.kind == SideKind.Delete) TextDecoration.LineThrough else null,
                ),
                color = if (view.kind == SideKind.Tombstone || view.kind == SideKind.Absent) c.onSurfaceVariant else if (view.kind == SideKind.Delete) c.onErrorContainer else c.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            when (view.kind) {
                SideKind.New -> Badge(strings.badgeNew, c.primary, c.onPrimary)
                SideKind.Delete -> Badge(strings.badgeDelete, c.error, c.onError)
                else -> {}
            }
        }
        view.lines.forEach { line ->
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), itemVerticalAlignment = Alignment.CenterVertically) {
                val style = SsdTheme.type.bodyMedium.copy(fontSize = if (wide) 14.sp else 12.sp, lineHeight = if (wide) 20.sp else 16.sp)
                if (line.old != null) {
                    Text(line.old, style = style.copy(textDecoration = TextDecoration.LineThrough), color = c.onSurfaceVariant)
                    Icon(Icons.ArrowForward, null, Modifier.size(14.dp), tint = c.onSurfaceVariant)
                    Text(line.text, style = style.copy(fontWeight = FontWeight.Bold), color = c.onSurface)
                } else {
                    Text(line.text, style = style, color = if (view.kind == SideKind.Delete) c.onErrorContainer else c.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun Badge(text: String, background: Color, color: Color) {
    Box(Modifier.clip(Pill).background(background).padding(horizontal = 10.dp, vertical = 2.dp)) {
        Text(text, style = SsdTheme.type.bodySmall.copy(fontWeight = FontWeight.Bold), color = color)
    }
}

@Composable
private fun Directions(
    row: SyncRow,
    direction: SyncDirection,
    label: String,
    strings: Strings,
    modifier: Modifier,
    vertical: Boolean,
    onDirection: (SyncDirection) -> Unit,
) {
    val c = SsdTheme.colors
    @Composable
    fun Option(value: SyncDirection, icon: ImageVector, description: String, itemModifier: Modifier) {
        val allowed = value in row.allowed
        val selected = value == direction
        val shape = if (selected) Pill else RoundedCornerShape(10.dp)
        Surface(
            onClick = { onDirection(value) },
            enabled = allowed,
            modifier = itemModifier.height(if (vertical) 40.dp else 48.dp).handCursor(allowed)
                .then(if (!allowed) Modifier.dashedBorder(1.dp, c.outlineVariant, 10.dp) else Modifier)
                .semantics {
                    role = Role.RadioButton
                    this.selected = selected
                    contentDescription = description
                },
            shape = shape,
            color = when {
                selected -> c.primary
                allowed -> c.secondaryContainer
                else -> Color.Transparent
            },
            contentColor = when {
                selected -> c.onPrimary
                allowed -> c.onSecondaryContainer
                else -> c.outline
            },
        ) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(if (vertical) 20.dp else 22.dp)) }
        }
    }
    val options = listOf(
        Triple(SyncDirection.ToApp, Icons.ArrowBack, strings.directionToApp),
        Triple(SyncDirection.Pause, Icons.Pause, strings.directionPause),
        Triple(SyncDirection.ToBackup, Icons.ArrowForward, strings.directionToBackup),
    )
    val groupModifier = modifier.semantics { contentDescription = strings.directionFor(label) }
    if (vertical) {
        Column(groupModifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            options.forEach { (value, icon, description) -> Option(value, icon, description, Modifier.fillMaxWidth()) }
        }
    } else {
        Row(groupModifier, horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally)) {
            options.forEach { (value, icon, description) -> Option(value, icon, description, Modifier.width(54.dp)) }
        }
    }
}
