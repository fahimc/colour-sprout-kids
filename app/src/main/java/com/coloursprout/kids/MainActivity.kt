package com.coloursprout.kids

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ColourSproutApp() }
    }
}

data class ColoringPage(
    val id: String,
    val title: String,
    val category: String,
    val difficulty: String,
    val lineArtPath: String,
    val maskPath: String,
    val thumbnailPath: String,
    val licenseId: String,
    val tags: List<String>,
)

data class SavedArtwork(
    val page: ColoringPage,
    val galleryPath: String,
    val lastEdited: String,
)

enum class Tool(val label: String) {
    Bucket("Fill"),
    Brush("Brush"),
    Crayon("Crayon"),
    Marker("Marker"),
    Glitter("Glitter"),
    Eraser("Erase"),
    EyeDropper("Pick"),
}

private sealed interface Screen {
    data object Home : Screen
    data object Browser : Screen
    data object Gallery : Screen
    data class Coloring(val page: ColoringPage) : Screen
    data class Finished(val page: ColoringPage, val previewPath: String?) : Screen
}

@Composable
fun ColourSproutApp() {
    val context = LocalContext.current
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    var pages by remember { mutableStateOf<List<ColoringPage>>(emptyList()) }
    fun navigateBack() {
        screen = when (screen) {
            Screen.Home -> Screen.Home
            Screen.Browser -> Screen.Home
            Screen.Gallery -> Screen.Browser
            is Screen.Coloring -> Screen.Browser
            is Screen.Finished -> Screen.Browser
        }
    }
    LaunchedEffect(Unit) {
        pages = withContext(Dispatchers.IO) { loadPages(context) }
    }
    MaterialTheme {
        BackHandler(enabled = screen != Screen.Home) {
            navigateBack()
        }
        Surface(Modifier.fillMaxSize(), color = Color(0xFFFFF4D7)) {
            when (val current = screen) {
                Screen.Home -> HomeScreen(onPlay = { screen = Screen.Browser }, onGallery = { screen = Screen.Gallery })
                Screen.Browser -> BrowserScreen(pages, onBack = ::navigateBack, onOpen = { screen = Screen.Coloring(it) }, onGallery = { screen = Screen.Gallery })
                Screen.Gallery -> GalleryScreen(
                    pages = pages,
                    onBack = ::navigateBack,
                    onOpen = { screen = Screen.Coloring(it) },
                )
                is Screen.Coloring -> ColoringScreen(
                    page = current.page,
                    onBack = ::navigateBack,
                    onFinished = { path -> screen = Screen.Finished(current.page, path) },
                )
                is Screen.Finished -> FinishedScreen(
                    page = current.page,
                    previewPath = current.previewPath,
                    onContinue = { screen = Screen.Coloring(current.page) },
                    onNew = { screen = Screen.Browser },
                    onGallery = { screen = Screen.Gallery },
                )
            }
        }
    }
}

private fun loadPages(context: Context): List<ColoringPage> {
    val assets = context.assets
    val result = mutableListOf<ColoringPage>()
    val categories = assets.list("coloring").orEmpty().filterNot { it.endsWith(".json") }
    for (category in categories) {
        for (pageDir in assets.list("coloring/$category").orEmpty()) {
            val path = "coloring/$category/$pageDir/metadata.json"
            val json = runCatching { assets.open(path).bufferedReader().use { it.readText() } }.getOrNull() ?: continue
            val obj = JSONObject(json)
            if (obj.optBoolean("needs_manual_mask", false)) continue
            val tags = obj.optJSONArray("tags")?.let { arr -> List(arr.length()) { arr.getString(it) } }.orEmpty()
            result += ColoringPage(
                id = obj.getString("id"),
                title = obj.getString("title"),
                category = obj.getString("category"),
                difficulty = obj.getString("difficulty"),
                lineArtPath = obj.getString("lineArtPath"),
                maskPath = obj.getString("maskPath"),
                thumbnailPath = obj.getString("thumbnailPath"),
                licenseId = obj.getString("licenseId"),
                tags = tags,
            )
        }
    }
    return result.sortedWith(compareBy({ it.category }, { it.title }))
}

private fun loadBitmap(context: Context, assetPath: String, mutable: Boolean = false): Bitmap {
    val bitmap = context.assets.open(assetPath).use { BitmapFactory.decodeStream(it) }
    return if (mutable) bitmap.copy(Bitmap.Config.ARGB_8888, true) else bitmap
}

@Composable
fun HomeScreen(onPlay: () -> Unit, onGallery: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFFFE9B6)),
    ) {
        Image(
            painter = painterResource(R.drawable.splash_art_room),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Column(
            modifier = Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CleanHomeLogo()
            Spacer(Modifier.height(34.dp))
            GamePlayButton(onPlay)
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onGallery,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF45B86B)),
                elevation = ButtonDefaults.buttonElevation(8.dp),
            ) {
                Text("GALLERY", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun CleanHomeLogo() {
    Column(
        modifier = Modifier
            .fillMaxWidth(0.88f)
            .background(Color(0xF7FFFFFF), RoundedCornerShape(34.dp))
            .border(4.dp, Color.White, RoundedCornerShape(34.dp))
            .padding(horizontal = 16.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        LogoWord("Colour", listOf(Color(0xFFFF2D8D), Color(0xFFFF8A00), Color(0xFFFFC928), Color(0xFF3AC65D), Color(0xFF1AA7FF), Color(0xFF7A4DFF)), 48.sp)
        Text("My", color = Color(0xFF8C28D9), fontSize = 34.sp, fontWeight = FontWeight.Black)
        LogoWord("World", listOf(Color(0xFF1AA7FF), Color(0xFF1167F2), Color(0xFF9B20D9), Color(0xFFFF2D55), Color(0xFFFF8A00)), 50.sp)
    }
}

@Composable
fun LogoWord(text: String, colors: List<Color>, fontSize: androidx.compose.ui.unit.TextUnit) {
    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        text.forEachIndexed { index, ch ->
            Text(
                ch.toString(),
                color = colors[index % colors.size],
                fontSize = fontSize,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
fun GamePlayButton(onPlay: () -> Unit) {
    Box(
        modifier = Modifier
            .size(136.dp)
            .clickable(onClick = onPlay),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Color(0x66000000), radius = size.minDimension * .43f, center = Offset(size.width * .52f, size.height * .56f))
            drawCircle(Color.White, radius = size.minDimension * .47f, center = center)
            drawCircle(Color(0xFFBCEAFF), radius = size.minDimension * .41f, center = Offset(size.width * .5f, size.height * .49f))
            drawCircle(Color(0xFFFFFFFF), radius = size.minDimension * .26f, center = Offset(size.width * .4f, size.height * .36f))
            val tri = Path().apply {
                moveTo(size.width * .43f, size.height * .32f)
                lineTo(size.width * .43f, size.height * .68f)
                lineTo(size.width * .72f, size.height * .5f)
                close()
            }
            drawPath(tri, Color(0xFF23C564))
        }
    }
}

@Composable
fun BrowserScreen(pages: List<ColoringPage>, onBack: () -> Unit, onOpen: (ColoringPage) -> Unit, onGallery: () -> Unit) {
    var selectedCategory by remember { mutableStateOf("animals") }
    var categoryPageIndex by remember { mutableIntStateOf(0) }
    val categories = pages.map { it.category }.distinct().ifEmpty { listOf("animals") }
    val maxCategoryPage = max(0, (categories.size - 1) / 2)
    val categoryPage = categoryPageIndex.coerceIn(0, maxCategoryPage)
    val visibleCategories = categories.drop(categoryPage * 2).take(2)
    val visible = pages.filter { it.category == selectedCategory }
    LaunchedEffect(categories) {
        if (selectedCategory !in categories) selectedCategory = categories.first()
        if (categoryPageIndex > maxCategoryPage) categoryPageIndex = maxCategoryPage
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFFFE2A8), Color(0xFFFFF7E6))))
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            HeaderButton("BACK", onBack, Color(0xFFFFF8E2), Color(0xFF6F4A29))
            HeaderButton("GALLERY", onGallery, Color(0xFF45B86B), Color.White, width = 116.dp)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Pick a picture",
            modifier = Modifier.fillMaxWidth(),
            fontSize = 34.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF5C3B22),
            textAlign = TextAlign.Start,
        )
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryArrowButton("<", enabled = categoryPage > 0) {
                val nextPage = (categoryPage - 1).coerceAtLeast(0)
                categoryPageIndex = nextPage
                selectedCategory = categories[nextPage * 2]
            }
            for (category in visibleCategories) {
                val chosen = category == selectedCategory
                Button(
                    onClick = { selectedCategory = category },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (chosen) Color(0xFF4CAF6C) else Color(0xFFFFF8E2)),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(categoryLabel(category), color = if (chosen) Color.White else Color(0xFF6F4A29), fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Center)
                }
            }
            if (visibleCategories.size < 2) Spacer(Modifier.weight(1f))
            CategoryArrowButton(">", enabled = categoryPage < maxCategoryPage) {
                val nextPage = (categoryPage + 1).coerceAtMost(maxCategoryPage)
                categoryPageIndex = nextPage
                selectedCategory = categories[nextPage * 2]
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().height(104.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            visibleCategories.forEach { category ->
                Card(
                    modifier = Modifier.weight(1f).fillMaxHeight().clickable { selectedCategory = category },
                    colors = CardDefaults.cardColors(containerColor = categoryColor(category)),
                    elevation = CardDefaults.cardElevation(7.dp),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.Center) {
                        Text(categoryIcon(category), fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Text(categoryLabel(category), fontSize = 20.sp, lineHeight = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                }
            }
            if (visibleCategories.size < 2) Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(visible) { page ->
                PageCard(page, onOpen)
            }
        }
    }
}

@Composable
fun HeaderButton(label: String, onClick: () -> Unit, color: Color, textColor: Color, width: androidx.compose.ui.unit.Dp = 88.dp) {
    Button(
        onClick = onClick,
        modifier = Modifier.width(width).height(46.dp),
        shape = RoundedCornerShape(18.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        elevation = ButtonDefaults.buttonElevation(5.dp),
    ) {
        Text(label, color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
    }
}

@Composable
fun CategoryArrowButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.width(44.dp).height(52.dp),
        shape = RoundedCornerShape(18.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFFFF8E2),
            disabledContainerColor = Color(0x66FFF8E2),
            contentColor = Color(0xFF6F4A29),
            disabledContentColor = Color(0x776F4A29),
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, disabledElevation = 0.dp),
    ) {
        Text(label, fontSize = 24.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
    }
}

@Composable
fun PageCard(page: ColoringPage, onOpen: (ColoringPage) -> Unit) {
    val context = LocalContext.current
    val thumb = remember(page.id) { loadBitmap(context, page.thumbnailPath).asImageBitmap() }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(.78f)
            .clickable { onOpen(page) },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFEF7)),
        elevation = CardDefaults.cardElevation(7.dp),
    ) {
        Box(Modifier.fillMaxSize().padding(10.dp)) {
            Image(thumb, contentDescription = page.title, modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Fit)
            Text(page.title, modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(), textAlign = TextAlign.Center, color = Color(0xFF5E442C), fontWeight = FontWeight.Bold)
            if (ProgressStore.isCompleted(context, page.id)) {
                Text("OK", modifier = Modifier.align(Alignment.TopEnd).background(Color(0xFF46B96A), CircleShape).size(28.dp), color = Color.White, textAlign = TextAlign.Center, fontWeight = FontWeight.Black, fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun GalleryScreen(pages: List<ColoringPage>, onBack: () -> Unit, onOpen: (ColoringPage) -> Unit) {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    val saved = remember(pages, refresh) { ProgressStore.gallery(context, pages) }
    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFFFE2A8), Color(0xFFBFE8C7))))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("BACK", fontSize = 18.sp, color = Color(0xFF6F4A29), fontWeight = FontWeight.Bold) }
            Text("Saved Gallery", modifier = Modifier.weight(1f), fontSize = 30.sp, fontWeight = FontWeight.Black, color = Color(0xFF6F4A29))
        }
        Spacer(Modifier.height(10.dp))
        if (saved.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Save a coloured picture first", color = Color(0xFF6F4A29), fontSize = 22.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(170.dp),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(saved, key = { "${it.page.id}-${it.lastEdited}" }) { art ->
                    SavedArtworkCard(
                        art = art,
                        onOpen = { onOpen(art.page) },
                        onExport = {
                            val uri = ProgressStore.exportSaved(context, art.page)
                            Toast.makeText(
                                context,
                                if (uri != null) "Exported PNG to Pictures/Colour My World" else "Could not export picture",
                                Toast.LENGTH_SHORT,
                            ).show()
                            refresh++
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun SavedArtworkCard(art: SavedArtwork, onOpen: () -> Unit, onExport: () -> Unit) {
    val preview = remember(art.galleryPath, art.lastEdited) { BitmapFactory.decodeFile(art.galleryPath)?.asImageBitmap() }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFEF7)),
        elevation = CardDefaults.cardElevation(7.dp),
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (preview != null) {
                Image(preview, contentDescription = art.page.title, modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Fit)
            } else {
                Box(Modifier.fillMaxWidth().aspectRatio(1f).background(Color(0xFFFFF2D6), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                    Text("Saved", color = Color(0xFF6F4A29), fontWeight = FontWeight.Black)
                }
            }
            Text(art.page.title, color = Color(0xFF5E442C), fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniButton("COLOUR", onOpen, Color(0xFFFFC94A), width = 74.dp)
                MiniButton("EXPORT", onExport, Color(0xFF45B86B), width = 74.dp)
            }
        }
    }
}

private fun categoryLabel(category: String) = category.split("_").joinToString(" ") { it.replaceFirstChar(Char::titlecase) }
private fun categoryIcon(category: String) = when (category) {
    "animals" -> "AN"
    "dinosaurs" -> "DI"
    "vehicles" -> "VE"
    "space" -> "SP"
    "fantasy" -> "FA"
    "sea_life" -> "SE"
    "nature" -> "NA"
    "cute_food" -> "FO"
    else -> "GO"
}

private fun categoryColor(category: String) = when (category) {
    "animals" -> Color(0xFF5FB96B)
    "dinosaurs" -> Color(0xFF8B7B43)
    "vehicles" -> Color(0xFF4E95D9)
    "space" -> Color(0xFF584DA8)
    "fantasy" -> Color(0xFFD36CA3)
    "sea_life" -> Color(0xFF2EA6A6)
    "nature" -> Color(0xFF83A83D)
    "cute_food" -> Color(0xFFE07E46)
    else -> Color(0xFF7D9B4E)
}

class ColoringSession(private val context: Context, val page: ColoringPage) {
    val lineBitmap: Bitmap = loadBitmap(context, page.lineArtPath)
    val maskBitmap: Bitmap = loadBitmap(context, page.maskPath)
    val paintBitmap: Bitmap = Bitmap.createBitmap(lineBitmap.width, lineBitmap.height, Bitmap.Config.ARGB_8888)
    val version: MutableIntState = mutableIntStateOf(0)
    val recentColors = mutableStateListOf(Color(0xFFFF6B5E), Color(0xFF4AA8FF), Color(0xFFFFC94A))
    val completed = mutableStateOf(ProgressStore.isCompleted(context, page.id))
    private val maskPixels: IntArray = IntArray(maskBitmap.width * maskBitmap.height)
    private val regionPixels: Map<Int, IntArray>
    private val blockedRegions: Set<Int>
    private val undo = ArrayDeque<Bitmap>()
    private val redo = ArrayDeque<Bitmap>()
    private val regionColours = mutableMapOf<String, String>()
    var activeRegion: Int = 0
    var selectedRegion: Int = 0

    init {
        maskBitmap.getPixels(maskPixels, 0, maskBitmap.width, 0, 0, maskBitmap.width, maskBitmap.height)
        val temp = HashMap<Int, MutableList<Int>>()
        maskPixels.forEachIndexed { index, raw ->
            if (AndroidColor.alpha(raw) > 0) {
                val key = raw and 0x00FFFFFF
                temp.getOrPut(key) { ArrayList() }.add(index)
            }
        }
        regionPixels = temp.mapValues { entry -> entry.value.toIntArray() }
        blockedRegions = findBorderRegions()
        ProgressStore.load(context, page.id, paintBitmap)
        version.intValue++
    }

    fun regionAt(x: Int, y: Int): Int {
        if (x !in 0 until maskBitmap.width || y !in 0 until maskBitmap.height) return 0
        val raw = maskPixels[y * maskBitmap.width + x]
        if (AndroidColor.alpha(raw) == 0) return 0
        val region = raw and 0x00FFFFFF
        return if (region in blockedRegions) 0 else region
    }

    fun fillRegion(region: Int, color: Color) {
        val pixels = regionPixels[region] ?: return
        snapshot()
        val argb = color.toArgb()
        for (idx in pixels) paintBitmap.setPixel(idx % paintBitmap.width, idx / paintBitmap.width, argb)
        regionColours[region.toString()] = "#%06X".format(argb and 0x00FFFFFF)
        rememberColor(color)
        selectedRegion = region
        changed()
    }

    fun clearRegion(region: Int) {
        val pixels = regionPixels[region] ?: return
        snapshot()
        for (idx in pixels) paintBitmap.setPixel(idx % paintBitmap.width, idx / paintBitmap.width, AndroidColor.TRANSPARENT)
        regionColours.remove(region.toString())
        selectedRegion = region
        changed()
    }

    fun clearAll() {
        snapshot()
        AndroidCanvas(paintBitmap).drawColor(AndroidColor.TRANSPARENT, PorterDuff.Mode.CLEAR)
        regionColours.clear()
        changed()
    }

    fun drawBrush(x: Int, y: Int, radius: Int, color: Color, tool: Tool) {
        val region = activeRegion.takeIf { it != 0 } ?: regionAt(x, y)
        if (region == 0) return
        activeRegion = region
        selectedRegion = region
        val r = max(2, radius)
        val colorInt = when (tool) {
            Tool.Eraser -> AndroidColor.TRANSPARENT
            Tool.Crayon -> color.copy(alpha = .72f).toArgb()
            Tool.Marker -> color.copy(alpha = .58f).toArgb()
            Tool.Glitter -> if (Random.nextFloat() > .72f) Color.White.toArgb() else color.toArgb()
            else -> color.toArgb()
        }
        for (yy in y - r..y + r) {
            if (yy !in 0 until paintBitmap.height) continue
            for (xx in x - r..x + r) {
                if (xx !in 0 until paintBitmap.width) continue
                val dx = xx - x
                val dy = yy - y
                if (dx * dx + dy * dy <= r * r && regionAt(xx, yy) == region) {
                    paintBitmap.setPixel(xx, yy, colorInt)
                }
            }
        }
        version.intValue++
    }

    fun beginStroke() {
        snapshot()
        activeRegion = 0
    }

    fun endStroke(color: Color, tool: Tool) {
        activeRegion = 0
        if (tool != Tool.Eraser) rememberColor(color)
        changed()
    }

    fun eyedrop(x: Int, y: Int): Color? {
        if (x !in 0 until paintBitmap.width || y !in 0 until paintBitmap.height) return null
        val raw = paintBitmap.getPixel(x, y)
        return if (AndroidColor.alpha(raw) == 0) null else Color(raw)
    }

    fun undo() {
        if (undo.isEmpty()) return
        redo.addLast(paintBitmap.copy(Bitmap.Config.ARGB_8888, true))
        restore(undo.removeLast())
    }

    fun redo() {
        if (redo.isEmpty()) return
        undo.addLast(paintBitmap.copy(Bitmap.Config.ARGB_8888, true))
        restore(redo.removeLast())
    }

    fun save(completedFlag: Boolean = false): String? {
        completed.value = completedFlag || completed.value
        ProgressStore.save(context, page.id, paintBitmap, completed.value, regionColours)
        return if (completed.value) ProgressStore.saveGalleryPreview(context, page, composeFinal())?.absolutePath else null
    }

    fun saveColoredArtwork(): String? {
        completed.value = true
        ProgressStore.save(context, page.id, paintBitmap, completed.value, regionColours)
        return ProgressStore.saveGalleryPreview(context, page, composeFinal())?.absolutePath
    }

    fun composeFinal(): Bitmap {
        val out = Bitmap.createBitmap(lineBitmap.width, lineBitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(out)
        canvas.drawColor(AndroidColor.WHITE)
        canvas.drawBitmap(paintBitmap, 0f, 0f, null)
        canvas.drawBitmap(lineBitmap, 0f, 0f, null)
        return out
    }

    private fun snapshot() {
        undo.addLast(paintBitmap.copy(Bitmap.Config.ARGB_8888, true))
        while (undo.size > 18) undo.removeFirst()
        redo.clear()
    }

    private fun restore(bitmap: Bitmap) {
        AndroidCanvas(paintBitmap).drawColor(AndroidColor.TRANSPARENT, PorterDuff.Mode.CLEAR)
        AndroidCanvas(paintBitmap).drawBitmap(bitmap, 0f, 0f, null)
        changed()
    }

    private fun rememberColor(color: Color) {
        recentColors.remove(color)
        recentColors.add(0, color)
        while (recentColors.size > 7) recentColors.removeAt(recentColors.lastIndex)
    }

    private fun changed() {
        version.intValue++
        ProgressStore.save(context, page.id, paintBitmap, completed.value, regionColours)
    }

    private fun findBorderRegions(): Set<Int> {
        val blocked = mutableSetOf<Int>()
        fun addIfRegion(x: Int, y: Int) {
            val raw = maskPixels[y * maskBitmap.width + x]
            if (AndroidColor.alpha(raw) > 0) blocked.add(raw and 0x00FFFFFF)
        }
        for (x in 0 until maskBitmap.width) {
            addIfRegion(x, 0)
            addIfRegion(x, maskBitmap.height - 1)
        }
        for (y in 0 until maskBitmap.height) {
            addIfRegion(0, y)
            addIfRegion(maskBitmap.width - 1, y)
        }
        return blocked
    }
}

@Composable
fun ColoringScreen(page: ColoringPage, onBack: () -> Unit, onFinished: (String?) -> Unit) {
    val context = LocalContext.current
    val session = remember(page.id) { ColoringSession(context, page) }
    var tool by remember { mutableStateOf(Tool.Bucket) }
    var selectedColor by remember { mutableStateOf(Color(0xFFFF6B5E)) }
    var brushSize by remember { mutableFloatStateOf(22f) }
    var hue by remember { mutableFloatStateOf(7f) }
    var saturation by remember { mutableFloatStateOf(.82f) }
    var value by remember { mutableFloatStateOf(1f) }
    var showClear by remember { mutableStateOf(false) }
    var showMixer by remember { mutableStateOf(false) }
    val config = LocalConfiguration.current
    val landscape = config.screenWidthDp > config.screenHeightDp
    fun saveArtwork(showFinished: Boolean = false) {
        val path = session.saveColoredArtwork()
        Toast.makeText(
            context,
            if (path != null) "Saved coloured picture to gallery" else "Could not save picture",
            Toast.LENGTH_SHORT,
        ).show()
        if (showFinished) onFinished(path)
    }

    if (showClear) {
        AlertDialog(
            onDismissRequest = { showClear = false },
            title = { Text("Clear page?") },
            text = { Text("Remove all colour from this picture.") },
            confirmButton = { TextButton(onClick = { session.clearAll(); showClear = false }) { Text("Clear") } },
            dismissButton = { TextButton(onClick = { showClear = false }) { Text("Cancel") } },
        )
    }

    if (landscape) {
        Row(
            Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(Color(0xFFB77A43), Color(0xFFFFE1AA), Color(0xFF78BE83))))
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ToolShelf(tool, onTool = { tool = it }, onBack = onBack, onSave = {
                saveArtwork()
            })
            ColoringCanvas(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                session = session,
                tool = tool,
                selectedColor = selectedColor,
                brushSize = brushSize.roundToInt(),
                onPickColor = { selectedColor = it },
            )
            ColorPanel(
                selectedColor = selectedColor,
                onColor = { selectedColor = it },
                hue = hue,
                onHue = {
                    hue = it
                    selectedColor = Color.hsv(hue, saturation, value)
                },
                saturation = saturation,
                onSaturation = {
                    saturation = it
                    selectedColor = Color.hsv(hue, saturation, value)
                },
                value = value,
                onValue = {
                    value = it
                    selectedColor = Color.hsv(hue, saturation, value)
                },
                brushSize = brushSize,
                onBrushSize = { brushSize = it },
                recent = session.recentColors,
                onUndo = session::undo,
                onRedo = session::redo,
                onClearArea = { session.clearRegion(session.selectedRegion) },
                onClearAll = { showClear = true },
                onDone = {
                    saveArtwork(showFinished = true)
                },
            )
        }
    } else {
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFFFFE1AA), Color(0xFF8BCB91))))
                .padding(8.dp),
        ) {
            ColoringCanvas(
                modifier = Modifier.fillMaxSize(),
                session = session,
                tool = tool,
                selectedColor = selectedColor,
                brushSize = brushSize.roundToInt(),
                onPickColor = { selectedColor = it },
            )
            ToolShelf(
                tool = tool,
                onTool = { tool = it },
                onBack = onBack,
                onSave = {
                    saveArtwork()
                },
                horizontal = true,
                modifier = Modifier.align(Alignment.TopCenter),
            )
            CompactColorBar(
                modifier = Modifier.align(Alignment.BottomCenter),
                selectedColor = selectedColor,
                onColor = { selectedColor = it },
                hue = hue,
                onHue = {
                    hue = it
                    selectedColor = Color.hsv(hue, saturation, value)
                },
                brushSize = brushSize,
                onBrushSize = { brushSize = it },
                recent = session.recentColors,
                onUndo = session::undo,
                onRedo = session::redo,
                onClearArea = { session.clearRegion(session.selectedRegion) },
                onClearAll = { showClear = true },
                onMixer = { showMixer = true },
                onDone = {
                    saveArtwork(showFinished = true)
                },
            )
            if (showMixer) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color(0x66000000))
                        .clickable { showMixer = false },
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(Modifier.padding(10.dp).clickable(enabled = false) {}) {
                        ColorPanel(
                            selectedColor = selectedColor,
                            onColor = { selectedColor = it },
                            hue = hue,
                            onHue = {
                                hue = it
                                selectedColor = Color.hsv(hue, saturation, value)
                            },
                            saturation = saturation,
                            onSaturation = {
                                saturation = it
                                selectedColor = Color.hsv(hue, saturation, value)
                            },
                            value = value,
                            onValue = {
                                value = it
                                selectedColor = Color.hsv(hue, saturation, value)
                            },
                            brushSize = brushSize,
                            onBrushSize = { brushSize = it },
                            recent = session.recentColors,
                            onUndo = session::undo,
                            onRedo = session::redo,
                            onClearArea = { session.clearRegion(session.selectedRegion) },
                            onClearAll = { showClear = true },
                            onDone = {
                                saveArtwork(showFinished = true)
                            },
                            compact = true,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ToolShelf(
    tool: Tool,
    onTool: (Tool) -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit,
    horizontal: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val tools = listOf(Tool.Brush, Tool.Bucket, Tool.Crayon, Tool.Marker, Tool.Glitter, Tool.Eraser, Tool.EyeDropper)
    val baseModifier = modifier.then(if (horizontal) Modifier.fillMaxWidth().height(62.dp) else Modifier.width(86.dp).fillMaxHeight())
    val arrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
    if (horizontal) {
        Row(
            baseModifier
                .background(Color(0xD07A4E2C), RoundedCornerShape(18.dp))
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ToolButton("BACK", false, onClick = onBack, width = 58.dp)
            Row(
                Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tools.forEach { ToolButton(toolLabel(it), tool == it, onClick = { onTool(it) }) }
            }
            ToolButton("SAVE", false, onClick = onSave, color = Color(0xFF45B86B), width = 62.dp)
        }
    } else {
        Column(baseModifier.background(Color(0xAA7A4E2C), RoundedCornerShape(22.dp)).padding(8.dp), verticalArrangement = arrangement, horizontalAlignment = Alignment.CenterHorizontally) {
            ToolButton("BACK", false, onClick = onBack, width = 68.dp)
            tools.forEach { ToolButton(toolShortLabel(it), tool == it, onClick = { onTool(it) }, width = 68.dp) }
            ToolButton("SAVE", false, onClick = onSave, color = Color(0xFF45B86B), width = 68.dp)
        }
    }
}

@Composable
fun ToolButton(label: String, selected: Boolean, onClick: () -> Unit, color: Color = Color(0xFFFFF5D7), width: androidx.compose.ui.unit.Dp = 64.dp) {
    Button(
        onClick = onClick,
        modifier = Modifier.width(width).height(46.dp),
        shape = RoundedCornerShape(16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 2.dp),
        colors = ButtonDefaults.buttonColors(containerColor = if (selected) Color(0xFFFFC94A) else color),
        elevation = ButtonDefaults.buttonElevation(7.dp),
    ) {
        Text(label, color = Color(0xFF5C3B22), fontSize = if (label.length > 5) 10.sp else 12.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, lineHeight = 11.sp)
    }
}

fun toolLabel(tool: Tool) = when (tool) {
    Tool.Bucket -> "FILL"
    Tool.Brush -> "BRUSH"
    Tool.Crayon -> "CRAYON"
    Tool.Marker -> "MARKER"
    Tool.Glitter -> "GLITTER"
    Tool.Eraser -> "ERASE"
    Tool.EyeDropper -> "PICK"
}

fun toolShortLabel(tool: Tool) = when (tool) {
    Tool.Bucket -> "FILL"
    Tool.Brush -> "BRUSH"
    Tool.Crayon -> "CRAY"
    Tool.Marker -> "MARK"
    Tool.Glitter -> "GLIT"
    Tool.Eraser -> "ERASE"
    Tool.EyeDropper -> "PICK"
}

@Composable
fun ColoringCanvas(
    modifier: Modifier,
    session: ColoringSession,
    tool: Tool,
    selectedColor: Color,
    brushSize: Int,
    onPickColor: (Color) -> Unit,
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val version = session.version.intValue
    val paintImage: ImageBitmap = remember(version) { session.paintBitmap.asImageBitmap() }
    val lineImage = remember(session.page.id) { session.lineBitmap.asImageBitmap() }
    var lastPoint by remember { mutableStateOf<Offset?>(null) }
    var zoom by remember(session.page.id) { mutableFloatStateOf(1f) }
    var pan by remember(session.page.id) { mutableStateOf(Offset.Zero) }

    Box(
        modifier
            .background(Color(0xFF7B5434), RoundedCornerShape(18.dp))
            .padding(10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            Modifier
                .fillMaxSize()
                .background(Color(0xFFFFFEFA), RoundedCornerShape(12.dp))
                .border(6.dp, Color(0xFFE5D0A6), RoundedCornerShape(12.dp))
                .pointerInput(tool, selectedColor, brushSize, canvasSize) {
                    awaitEachGesture {
                        var strokeStarted = false
                        var didMove = false
                        while (true) {
                            val event = awaitPointerEvent()
                            val pressed = event.changes.filter { it.pressed }
                            if (pressed.isEmpty()) {
                                if (strokeStarted) session.endStroke(selectedColor, tool)
                                if (!strokeStarted && !didMove && lastPoint != null) {
                                    val point = mapToImage(lastPoint!!, canvasSize, session.lineBitmap.width, session.lineBitmap.height, zoom, pan)
                                    if (point != null) {
                                        when (tool) {
                                            Tool.Bucket -> session.fillRegion(session.regionAt(point.first, point.second), selectedColor)
                                            Tool.EyeDropper -> session.eyedrop(point.first, point.second)?.let(onPickColor)
                                            Tool.Eraser -> session.clearRegion(session.regionAt(point.first, point.second))
                                            else -> {
                                                session.beginStroke()
                                                session.drawBrush(point.first, point.second, brushSize, selectedColor, tool)
                                                session.endStroke(selectedColor, tool)
                                            }
                                        }
                                    }
                                }
                                lastPoint = null
                                break
                            }

                            if (pressed.size > 1) {
                                if (strokeStarted) {
                                    session.endStroke(selectedColor, tool)
                                    strokeStarted = false
                                }
                                val oldZoom = zoom
                                val newZoom = (zoom * event.calculateZoom()).coerceIn(1f, 5f)
                                val centroid = event.calculateCentroid(useCurrent = false)
                                val canvasCenter = Offset(canvasSize.width / 2f, canvasSize.height / 2f)
                                val zoomFocusShift = (centroid - canvasCenter - pan) * (1f - newZoom / oldZoom)
                                zoom = newZoom
                                pan = clampPan(
                                    canvasSize.width.toFloat(),
                                    canvasSize.height.toFloat(),
                                    session.lineBitmap.width,
                                    session.lineBitmap.height,
                                    zoom,
                                    pan + event.calculatePan() + zoomFocusShift,
                                )
                                lastPoint = null
                                didMove = true
                                event.changes.forEach { it.consume() }
                            } else {
                                val current = pressed.first().position
                                val previous = lastPoint
                                if (previous == null) {
                                    lastPoint = current
                                } else if (tool !in listOf(Tool.Bucket, Tool.EyeDropper)) {
                                    if (!strokeStarted) {
                                        session.beginStroke()
                                        val start = mapToImage(previous, canvasSize, session.lineBitmap.width, session.lineBitmap.height, zoom, pan)
                                        if (start != null) {
                                            session.activeRegion = session.regionAt(start.first, start.second)
                                            session.drawBrush(start.first, start.second, brushSize, selectedColor, tool)
                                        }
                                        strokeStarted = true
                                    }
                                    val steps = max(1, ceil((current - previous).getDistance() / max(3f, brushSize / 2f)).toInt())
                                    for (i in 1..steps) {
                                        val t = i / steps.toFloat()
                                        val mixed = Offset(previous.x + (current.x - previous.x) * t, previous.y + (current.y - previous.y) * t)
                                        val p = mapToImage(mixed, canvasSize, session.lineBitmap.width, session.lineBitmap.height, zoom, pan)
                                        if (p != null) session.drawBrush(p.first, p.second, brushSize, selectedColor, tool)
                                    }
                                    lastPoint = current
                                    didMove = true
                                    pressed.first().consume()
                                } else {
                                    if ((current - previous).getDistance() > 8f) didMove = true
                                    lastPoint = current
                                }
                            }
                        }
                    }
                },
        ) {
            canvasSize = IntSize(size.width.roundToInt(), size.height.roundToInt())
            pan = clampPan(size.width, size.height, session.lineBitmap.width, session.lineBitmap.height, zoom, pan)
            val dst = transformedRect(size.width, size.height, session.lineBitmap.width, session.lineBitmap.height, zoom, pan)
            drawRect(Color.White, topLeft = dst.topLeft, size = dst.size)
            drawImage(paintImage, dstOffset = androidx.compose.ui.unit.IntOffset(dst.left.roundToInt(), dst.top.roundToInt()), dstSize = IntSize(dst.width.roundToInt(), dst.height.roundToInt()))
            drawImage(lineImage, dstOffset = androidx.compose.ui.unit.IntOffset(dst.left.roundToInt(), dst.top.roundToInt()), dstSize = IntSize(dst.width.roundToInt(), dst.height.roundToInt()))
            if (zoom > 1.02f) {
                drawRoundRect(
                    Color(0xCC5C3B22),
                    topLeft = Offset(18f, 18f),
                    size = androidx.compose.ui.geometry.Size(104f, 42f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(21f, 21f),
                )
                drawContext.canvas.nativeCanvas.drawText(
                    "${(zoom * 100).roundToInt()}%",
                    42f,
                    46f,
                    Paint().apply {
                        color = AndroidColor.WHITE
                        textSize = 24f
                        isFakeBoldText = true
                    },
                )
            }
        }
    }
}

@Composable
fun ColorPanel(
    selectedColor: Color,
    onColor: (Color) -> Unit,
    hue: Float,
    onHue: (Float) -> Unit,
    saturation: Float,
    onSaturation: (Float) -> Unit,
    value: Float,
    onValue: (Float) -> Unit,
    brushSize: Float,
    onBrushSize: (Float) -> Unit,
    recent: List<Color>,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onClearArea: () -> Unit,
    onClearAll: () -> Unit,
    onDone: () -> Unit,
    compact: Boolean = false,
) {
    val swatches = listOf(
        Color(0xFFFF3B30), Color(0xFFFF9500), Color(0xFFFFCC00), Color(0xFF34C759),
        Color(0xFF00C7BE), Color(0xFF007AFF), Color(0xFF5856D6), Color(0xFFFF2D55),
        Color(0xFFFFFFFF), Color(0xFF8E8E93), Color(0xFF3A2A20), Color(0xFF000000),
    )
    Column(
        Modifier
            .then(if (compact) Modifier.fillMaxWidth() else Modifier.width(238.dp).fillMaxHeight())
            .background(Color(0xCC704A2C), RoundedCornerShape(22.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(54.dp).background(selectedColor, CircleShape).border(4.dp, Color.White, CircleShape))
            Text("Colour", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
        }
        Text("Hue", color = Color.White, fontWeight = FontWeight.Bold)
        Slider(value = hue, onValueChange = onHue, valueRange = 0f..360f)
        Text("Bright", color = Color.White, fontWeight = FontWeight.Bold)
        Slider(value = value, onValueChange = onValue, valueRange = .1f..1f)
        Text("Soft", color = Color.White, fontWeight = FontWeight.Bold)
        Slider(value = saturation, onValueChange = onSaturation, valueRange = 0f..1f)
        Text("Size", color = Color.White, fontWeight = FontWeight.Bold)
        Slider(value = brushSize, onValueChange = onBrushSize, valueRange = 5f..54f)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            swatches.forEach { swatch ->
                Box(Modifier.size(34.dp).background(swatch, CircleShape).border(2.dp, Color.White, CircleShape).clickable { onColor(swatch) })
            }
        }
        AnimatedVisibility(recent.isNotEmpty()) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                recent.forEach { swatch ->
                    Box(Modifier.size(30.dp).background(swatch, CircleShape).border(2.dp, Color(0xFFFFE8A8), CircleShape).clickable { onColor(swatch) })
                }
            }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniButton("UNDO", onUndo)
            MiniButton("REDO", onRedo)
            MiniButton("AREA", onClearArea)
            MiniButton("ALL", onClearAll)
            MiniButton("SAVE", onDone, Color(0xFF45B86B))
        }
    }
}

@Composable
fun MiniButton(label: String, onClick: () -> Unit, color: Color = Color(0xFFFFF5D7), width: androidx.compose.ui.unit.Dp = 58.dp) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.width(width).height(42.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 2.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        elevation = ButtonDefaults.buttonElevation(5.dp),
    ) {
        Text(label, color = Color(0xFF5C3B22), fontWeight = FontWeight.Black, fontSize = 11.sp, textAlign = TextAlign.Center, lineHeight = 11.sp)
    }
}

@Composable
fun FinishedScreen(page: ColoringPage, previewPath: String?, onContinue: () -> Unit, onNew: () -> Unit, onGallery: () -> Unit) {
    val context = LocalContext.current
    val transition = rememberInfiniteTransition(label = "confetti")
    val t by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(1400), RepeatMode.Restart), label = "confetti")
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFFFF0B8), Color(0xFF85D49B))))
            .padding(20.dp),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            repeat(50) { i ->
                val x = ((i * 73) % 100) / 100f * size.width
                val y = (((i * 47) % 100) / 100f * size.height + t * size.height) % size.height
                drawRect(listOf(Color.Red, Color.Blue, Color.Yellow, Color.Green, Color.Magenta)[i % 5], topLeft = Offset(x, y), size = androidx.compose.ui.geometry.Size(12f, 20f))
            }
        }
        Column(Modifier.align(Alignment.Center).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Saved!", fontSize = 44.sp, color = Color(0xFF2D6E42), fontWeight = FontWeight.Black)
            Text(page.title, fontSize = 26.sp, color = Color(0xFF5C3B22), fontWeight = FontWeight.Bold)
            Button(onClick = onContinue, shape = RoundedCornerShape(24.dp)) { Text("Continue colouring") }
            Button(onClick = onGallery, shape = RoundedCornerShape(24.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF45B86B))) { Text("Open gallery") }
            Button(
                onClick = {
                    val uri = ProgressStore.exportSaved(context, page)
                    Toast.makeText(
                        context,
                        if (uri != null) "Exported PNG to Pictures/Colour My World" else "Could not export picture",
                        Toast.LENGTH_SHORT,
                    ).show()
                },
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4E95D9)),
            ) { Text("Export PNG") }
            Button(onClick = onNew, shape = RoundedCornerShape(24.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B5E))) { Text("Start new picture") }
            Text("Saved in the app gallery. Export PNG writes to Pictures/Colour My World.", textAlign = TextAlign.Center, color = Color(0xFF5C3B22), fontWeight = FontWeight.Bold)
            if (previewPath != null) Text(previewPath, textAlign = TextAlign.Center, color = Color(0xFF5C3B22), fontSize = 12.sp)
        }
    }
}

private fun fitRect(canvasWidth: Float, canvasHeight: Float, imageWidth: Int, imageHeight: Int): Rect {
    val scale = min(canvasWidth / imageWidth, canvasHeight / imageHeight)
    val w = imageWidth * scale
    val h = imageHeight * scale
    val left = (canvasWidth - w) / 2f
    val top = (canvasHeight - h) / 2f
    return Rect(left, top, left + w, top + h)
}

private fun transformedRect(canvasWidth: Float, canvasHeight: Float, imageWidth: Int, imageHeight: Int, zoom: Float, pan: Offset): Rect {
    val base = fitRect(canvasWidth, canvasHeight, imageWidth, imageHeight)
    val w = base.width * zoom
    val h = base.height * zoom
    val cx = canvasWidth / 2f + pan.x
    val cy = canvasHeight / 2f + pan.y
    return Rect(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f)
}

private fun clampPan(canvasWidth: Float, canvasHeight: Float, imageWidth: Int, imageHeight: Int, zoom: Float, pan: Offset): Offset {
    val base = fitRect(canvasWidth, canvasHeight, imageWidth, imageHeight)
    val zoomedWidth = base.width * zoom
    val zoomedHeight = base.height * zoom
    val maxX = max(0f, (zoomedWidth - canvasWidth) / 2f)
    val maxY = max(0f, (zoomedHeight - canvasHeight) / 2f)
    return Offset(pan.x.coerceIn(-maxX, maxX), pan.y.coerceIn(-maxY, maxY))
}

private fun mapToImage(
    offset: Offset,
    canvasSize: IntSize,
    imageWidth: Int,
    imageHeight: Int,
    zoom: Float = 1f,
    pan: Offset = Offset.Zero,
): Pair<Int, Int>? {
    if (canvasSize.width == 0 || canvasSize.height == 0) return null
    val rect = transformedRect(canvasSize.width.toFloat(), canvasSize.height.toFloat(), imageWidth, imageHeight, zoom, pan)
    if (!rect.contains(offset)) return null
    val x = ((offset.x - rect.left) / rect.width * imageWidth).roundToInt().coerceIn(0, imageWidth - 1)
    val y = ((offset.y - rect.top) / rect.height * imageHeight).roundToInt().coerceIn(0, imageHeight - 1)
    return x to y
}

@Composable
fun CompactColorBar(
    modifier: Modifier,
    selectedColor: Color,
    onColor: (Color) -> Unit,
    hue: Float,
    onHue: (Float) -> Unit,
    brushSize: Float,
    onBrushSize: (Float) -> Unit,
    recent: List<Color>,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onClearArea: () -> Unit,
    onClearAll: () -> Unit,
    onMixer: () -> Unit,
    onDone: () -> Unit,
) {
    val swatches = listOf(
        Color(0xFFFF3B30), Color(0xFFFF9500), Color(0xFFFFCC00), Color(0xFF34C759),
        Color(0xFF00C7BE), Color(0xFF007AFF), Color(0xFF5856D6), Color(0xFFFF2D55),
        Color(0xFFFFFFFF), Color(0xFF8E8E93), Color(0xFF3A2A20), Color(0xFF000000),
    )
    Column(
        modifier
            .fillMaxWidth()
            .background(Color(0xE0704A2C), RoundedCornerShape(18.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(40.dp).background(selectedColor, CircleShape).border(3.dp, Color.White, CircleShape))
            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (swatches + recent).distinct().forEach { swatch ->
                    Box(Modifier.size(32.dp).background(swatch, CircleShape).border(2.dp, Color.White, CircleShape).clickable { onColor(swatch) })
                }
            }
            MiniButton("MIX", onMixer, Color(0xFFFFC94A), width = 52.dp)
            MiniButton("SAVE", onDone, Color(0xFF45B86B), width = 64.dp)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Hue", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(30.dp))
            Slider(value = hue, onValueChange = onHue, valueRange = 0f..360f, modifier = Modifier.weight(1f))
            Text("Size", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(30.dp))
            Slider(value = brushSize, onValueChange = onBrushSize, valueRange = 5f..54f, modifier = Modifier.weight(1f))
        }
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MiniButton("UNDO", onUndo)
            MiniButton("REDO", onRedo)
            MiniButton("AREA", onClearArea)
            MiniButton("ALL", onClearAll)
        }
    }
}

object ProgressStore {
    private fun dir(context: Context): File = File(context.filesDir, "progress").also { it.mkdirs() }
    private fun galleryDir(context: Context): File = File(context.filesDir, "gallery").also { it.mkdirs() }
    private fun png(context: Context, pageId: String) = File(dir(context), "$pageId.png")
    private fun json(context: Context, pageId: String) = File(dir(context), "$pageId.json")
    private fun galleryPng(context: Context, pageId: String) = File(galleryDir(context), "$pageId-final.png")

    fun save(context: Context, pageId: String, paintLayer: Bitmap, completed: Boolean, regionColours: Map<String, String> = emptyMap()) {
        FileOutputStream(png(context, pageId)).use { paintLayer.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val coloursJson = JSONObject()
        regionColours.forEach { (region, color) -> coloursJson.put(region, color) }
        val old = runCatching { JSONObject(json(context, pageId).readText()) }.getOrNull()
        val galleryPath = old?.optString("galleryPath").orEmpty().ifBlank { galleryPng(context, pageId).absolutePath }
        json(context, pageId).writeText(
            JSONObject()
                .put("pageId", pageId)
                .put("paintLayerPath", png(context, pageId).absolutePath)
                .put("galleryPath", galleryPath)
                .put("regionColours", coloursJson)
                .put("lastEdited", Instant.now().toString())
                .put("completed", completed)
                .toString(2),
        )
    }

    fun load(context: Context, pageId: String, target: Bitmap) {
        val file = png(context, pageId)
        if (!file.exists()) return
        val saved = BitmapFactory.decodeFile(file.absolutePath) ?: return
        AndroidCanvas(target).drawBitmap(saved, 0f, 0f, null)
    }

    fun isCompleted(context: Context, pageId: String): Boolean {
        val file = json(context, pageId)
        return runCatching { JSONObject(file.readText()).optBoolean("completed", false) }.getOrDefault(false)
    }

    fun saveGalleryPreview(context: Context, page: ColoringPage, bitmap: Bitmap): File? {
        val out = galleryPng(context, page.id)
        return runCatching {
            FileOutputStream(out).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            val progress = json(context, page.id)
            val obj = runCatching { JSONObject(progress.readText()) }.getOrDefault(JSONObject().put("pageId", page.id))
            obj.put("completed", true)
                .put("galleryPath", out.absolutePath)
                .put("lastEdited", Instant.now().toString())
            progress.writeText(obj.toString(2))
            out
        }.getOrNull()
    }

    fun gallery(context: Context, pages: List<ColoringPage>): List<SavedArtwork> {
        val byId = pages.associateBy { it.id }
        return dir(context).listFiles { file -> file.extension == "json" }.orEmpty().mapNotNull { file ->
            val obj = runCatching { JSONObject(file.readText()) }.getOrNull() ?: return@mapNotNull null
            if (!obj.optBoolean("completed", false)) return@mapNotNull null
            val page = byId[obj.optString("pageId")] ?: return@mapNotNull null
            val path = obj.optString("galleryPath").ifBlank { galleryPng(context, page.id).absolutePath }
            if (!File(path).exists()) {
                val composed = composeSavedBitmap(context, page) ?: return@mapNotNull null
                saveGalleryPreview(context, page, composed)
            }
            val finalPath = obj.optString("galleryPath").ifBlank { galleryPng(context, page.id).absolutePath }
            if (!File(finalPath).exists()) return@mapNotNull null
            SavedArtwork(page, finalPath, obj.optString("lastEdited"))
        }.sortedByDescending { it.lastEdited }
    }

    fun exportSaved(context: Context, page: ColoringPage): Uri? {
        val bitmap = composeSavedBitmap(context, page) ?: BitmapFactory.decodeFile(galleryPng(context, page.id).absolutePath) ?: return null
        return exportPng(context, page, bitmap)
    }

    private fun composeSavedBitmap(context: Context, page: ColoringPage): Bitmap? {
        val line = runCatching { loadBitmap(context, page.lineArtPath) }.getOrNull() ?: return null
        val out = Bitmap.createBitmap(line.width, line.height, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(out)
        canvas.drawColor(AndroidColor.WHITE)
        val savedPaint = BitmapFactory.decodeFile(png(context, page.id).absolutePath)
        if (savedPaint != null) canvas.drawBitmap(savedPaint, 0f, 0f, null)
        canvas.drawBitmap(line, 0f, 0f, null)
        return out
    }
}

fun exportPng(context: Context, page: ColoringPage, bitmap: Bitmap): Uri? {
    val name = "ColourMyWorld-${page.id}-${System.currentTimeMillis()}.png"
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Colour My World")
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        context.contentResolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        uri
    } else {
        val dir = File(context.getExternalFilesDir(null), "exports").also { it.mkdirs() }
        val file = File(dir, name)
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        Uri.fromFile(file)
    }
}
