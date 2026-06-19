package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.engine.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

// History Record data model for logging calculations offline
data class HistoryRecord(
    val timestamp: String,
    val category: String,
    val details: String,
    val result: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(dynamicColor = false, darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = GeoBackground // Clean sleek slate body background
                ) {
                    BioSciCasioApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BioSciCasioApp() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Application state
    var currentMenuOption by remember { mutableStateOf(0) } // 0 = Main Menu, 1-17 = Tools, 18 = Exit (Power Down)
    var terminalStream = remember { mutableStateListOf<String>() }
    val historyList = remember { mutableStateListOf<HistoryRecord>() }
    var historySearchQuery by remember { mutableStateOf("") }
    
    // Command terminal raw line text
    var commandInputText by remember { mutableStateOf("") }

    // Initialize console screen with greeting
    LaunchedEffect(Unit) {
        if (terminalStream.isEmpty()) {
            terminalStream.add("===========================================")
            terminalStream.add("         BIOSCI CASIO X CORE V4.0          ")
            terminalStream.add("   OFFLINE SCIENTIFIC & MOLECULAR ENGINE   ")
            terminalStream.add("===========================================")
            terminalStream.add("SYSTEMS ALL SECURE. NO INTERNET REQUIRED.")
            terminalStream.add("READY. ENTER OPTION [1-17] OR CLICK BELOW.")
            terminalStream.add("-------------------------------------------")
        }
    }

    // Listens to terminal stream and automatically scroll
    val terminalListState = rememberLazyListState()
    LaunchedEffect(terminalStream.size) {
        if (terminalStream.isNotEmpty()) {
            terminalListState.animateScrollToItem(terminalStream.size - 1)
        }
    }

    fun executeTerminalCommand(cmd: String) {
        val trimmed = cmd.trim()
        if (trimmed.isEmpty()) return
        terminalStream.add("> Selected: Option $trimmed")

        val option = trimmed.toIntOrNull()
        if (option != null && option in 1..18) {
            currentMenuOption = option
            if (option == 18) {
                terminalStream.add("[POWERING DOWN BIOSCI CASIO X ENGINE...]")
                terminalStream.add("GOODBYE.")
            } else {
                terminalStream.add("• Loaded Module $option: ${getMenuTitle(option)}")
            }
        } else {
            terminalStream.add("ERROR: INVALID COMMAND. ENTER 1-18.")
        }
        commandInputText = ""
    }

    // Screen content based on loaded option
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. BIOSCI CASIO SYSTEM STATUS HEADER (Geometric Balance)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Green status led
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(GeoEmeraldAccent, shape = androidx.compose.foundation.shape.CircleShape)
                )
                Text(
                    text = "BIOSCI CASIO X V4.0",
                    color = GeoTextWhite.copy(alpha = 0.6f),
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Custom battery icon indicator
                Box(
                    modifier = Modifier
                        .size(width = 16.dp, height = 10.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                        .padding(1.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.75f)
                            .background(Color.White)
                    )
                }
                
                // Real-time HH:mm status
                val clockTime = remember { mutableStateOf("") }
                LaunchedEffect(Unit) {
                    while (true) {
                        clockTime.value = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                        kotlinx.coroutines.delay(1000)
                    }
                }
                Text(
                    text = clockTime.value.ifEmpty { "14:28" },
                    color = GeoTextWhite.copy(alpha = 0.8f),
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                )
            }
        }

        // 2. GEOMETRIC BALANCE LCD WORKSTATION SCREEN (Matte Black with Emerald details)
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .fillMaxWidth()
                .weight(1.8f)
                .background(GeoMonitorBlack, RoundedCornerShape(16.dp))
                .border(1.dp, GeoBorderSlate, RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Monitor header info bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val currentModuleLabel = remember(currentMenuOption) {
                    if (currentMenuOption == 0) "Solver: General Menu"
                    else "Solver: " + getMenuTitle(currentMenuOption)
                }
                Text(
                    text = currentModuleLabel.uppercase(),
                    color = GeoEmeraldAccent.copy(alpha = 0.8f),
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.2).sp
                    )
                )
                
                // Offline tag box
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(GeoSurfaceSlate)
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "OFFLINE",
                        color = GeoTextWhite,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }
            
            // Middle section mapping LCD lines or current results
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 6.dp)
            ) {
                if (currentMenuOption == 18) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "SYSTEM SHUTDOWN",
                                color = Color.Gray,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    currentMenuOption = 0
                                    terminalStream.add("[REBOOTING SECURE OFFLINE SYSTEM...]")
                                    terminalStream.add("BIOSCI CASIO X READY.")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF335E3B)),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.testTag("reboot_button")
                            ) {
                                Text("START SOLVER", color = Color(0xFF9EFFAC), fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = terminalListState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(terminalStream) { logLine ->
                            Text(
                                text = logLine,
                                color = getLineColor(logLine),
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp
                                )
                            )
                        }
                    }
                }
            }
            
            // Screen bottom horizontal rule + Route indicator
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(GeoEmeraldAccent.copy(alpha = 0.12f))
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val statusRouteText = remember(currentMenuOption) {
                        if (currentMenuOption == 0) "ROUTE: [MENU] → [CHOOSE_SOLVER] → [RUN]"
                        else "ROUTE: [M_" + currentMenuOption + "] → [INPUT] → [SOLVED]"
                    }
                    Text(
                        text = statusRouteText.uppercase(),
                        color = GeoTextWhite.copy(alpha = 0.5f),
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp
                        )
                    )
                    
                    val checksumText = remember(currentMenuOption) {
                        "ID: " + (8800 + currentMenuOption) + "-A"
                    }
                    Text(
                        text = checksumText,
                        color = GeoTextWhite.copy(alpha = 0.5f),
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp
                        )
                    )
                }
            }
        }

        // 3. ACTIVE SOLVER MODULE CABINET (Sleek dark container with fine slate borders)
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .fillMaxWidth()
                .weight(2.6f)
                .clip(RoundedCornerShape(24.dp))
                .background(GeoDarkGrey)
                .border(1.dp, GeoBorderSlate, RoundedCornerShape(24.dp))
                .padding(14.dp)
        ) {
            AnimatedContent(
                targetState = currentMenuOption,
                transitionSpec = {
                    fadeIn(animationSpec = tween(150)) togetherWith fadeOut(animationSpec = tween(150))
                }, label = "ModuleNavigation"
            ) { targetOption ->
                when (targetOption) {
                    0 -> MainMenuListView { optionNum ->
                        executeTerminalCommand(optionNum.toString())
                    }
                    1 -> ScientificCalculatorUi(
                        terminalStream = terminalStream,
                        historySave = { calc, ans ->
                            historyList.add(HistoryRecord(getTimestamp(), "Scientific Calc", calc, ans))
                        }
                    )
                    2 -> ChemistrySolverUi(
                        terminalStream = terminalStream,
                        historySave = { details, ans ->
                            historyList.add(HistoryRecord(getTimestamp(), "Chemistry Solver", details, ans))
                        }
                    )
                    3 -> PhysicalChemistryUi(
                        terminalStream = terminalStream,
                        historySave = { details, ans ->
                            historyList.add(HistoryRecord(getTimestamp(), "Physical Chem", details, ans))
                        }
                    )
                    4 -> ThermodynamicsUi(
                        terminalStream = terminalStream,
                        historySave = { details, ans ->
                            historyList.add(HistoryRecord(getTimestamp(), "Thermodynamics", details, ans))
                        }
                    )
                    5 -> GasLawsUi(
                        terminalStream = terminalStream,
                        historySave = { details, ans ->
                            historyList.add(HistoryRecord(getTimestamp(), "Gas Laws", details, ans))
                        }
                    )
                    6 -> ChemicalEquilibriumUi(
                        terminalStream = terminalStream,
                        historySave = { details, ans ->
                            historyList.add(HistoryRecord(getTimestamp(), "Chemical Equilibrium", details, ans))
                        }
                    )
                    7 -> ElectrochemistryUi(
                        terminalStream = terminalStream,
                        historySave = { details, ans ->
                            historyList.add(HistoryRecord(getTimestamp(), "Electrochemistry", details, ans))
                        }
                    )
                    8 -> ChemicalKineticsUi(
                        terminalStream = terminalStream,
                        historySave = { details, ans ->
                            historyList.add(HistoryRecord(getTimestamp(), "Chemical Kinetics", details, ans))
                        }
                    )
                    9 -> ColligativePropertiesUi(
                        terminalStream = terminalStream,
                        historySave = { details, ans ->
                            historyList.add(HistoryRecord(getTimestamp(), "Colligative Properties", details, ans))
                        }
                    )
                    10 -> PeriodicTableExplorerUi()
                    11 -> ChemicalFormulaAnalyzerUi(
                        terminalStream = terminalStream,
                        historySave = { details, ans ->
                            historyList.add(HistoryRecord(getTimestamp(), "Formula Analyzer", details, ans))
                        }
                    )
                    12 -> SolutionPreparationUi(
                        terminalStream = terminalStream,
                        historySave = { details, ans ->
                            historyList.add(HistoryRecord(getTimestamp(), "Solution Prep", details, ans))
                        }
                    )
                    13 -> BioinformaticsUi(
                        terminalStream = terminalStream,
                        historySave = { details, ans ->
                            historyList.add(HistoryRecord(getTimestamp(), "Bioinformatics", details, ans))
                        }
                    )
                    14 -> UnitConverterUi(
                        terminalStream = terminalStream,
                        historySave = { details, ans ->
                            historyList.add(HistoryRecord(getTimestamp(), "Unit Conversion", details, ans))
                        }
                    )
                    15 -> FormulaDatabaseUi(
                        onSelectFormula = { record ->
                            terminalStream.add("======================")
                            terminalStream.add("SELECTED FORMULA:")
                            terminalStream.add("• Name: ${record.name}")
                            terminalStream.add("• Expr: ${record.formulaString}")
                            terminalStream.add("• About: ${record.description}")
                            terminalStream.add("======================")
                        }
                    )
                    16 -> HistoryUi(
                        historyRecords = historyList,
                        searchQuery = historySearchQuery,
                        onSearchChange = { historySearchQuery = it },
                        onShareCsv = {
                            val csvFile = buildHistoryCsvFile(context, historyList)
                            if (csvFile != null) {
                                triggerFileShare(context, csvFile)
                            } else {
                                Toast.makeText(context, "History is empty!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onClear = {
                            historyList.clear()
                            terminalStream.add("[CALCULATION LIST HISTORY TRUNCATED/CLEARED]")
                        }
                    )
                    17 -> GraphPlotterUi()
                    else -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Module Down. Press RESET below.", color = Color.White)
                    }
                }
            }
        }

        // 4. GEOMETRIC BOTTOM ACTION BAR (Styled like a modern workstation console keyboard)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(GeoSurfaceSlate, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .border(1.dp, GeoBorderSlate, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back to Main Menu Button
                Button(
                    onClick = { 
                        currentMenuOption = 0 
                        terminalStream.add("--- RETURNED TO MAIN CHASSIS MENU ---")
                    },
                    enabled = currentMenuOption != 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GeoAccentBlue, 
                        disabledContainerColor = GeoSurfaceSlate.copy(alpha = 0.5f),
                        contentColor = GeoAccentNavy,
                        disabledContentColor = GeoTextWhite.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(44.dp)
                        .testTag("menu_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Main Menu",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "MENU", 
                        fontWeight = FontWeight.Bold, 
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }

                // Command direct numeric terminal input line
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = commandInputText,
                        onValueChange = { commandInputText = it },
                        placeholder = { Text("Command (1-18)...", color = GeoTextWhite.copy(alpha = 0.4f), fontSize = 11.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GeoAccentBlue,
                            unfocusedBorderColor = GeoBorderSlate,
                            focusedContainerColor = GeoMonitorBlack,
                            unfocusedContainerColor = GeoMonitorBlack,
                            focusedTextColor = GeoTextWhite,
                            unfocusedTextColor = GeoTextWhite
                        ),
                        textStyle = TextStyle(color = GeoTextWhite, fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("cli_input"),
                        trailingIcon = {
                            IconButton(onClick = { executeTerminalCommand(commandInputText) }) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Run", tint = GeoEmeraldAccent)
                            }
                        }
                    )
                }

                // Physical style power/reboot trigger
                Button(
                    onClick = {
                        executeTerminalCommand("18")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBC3434)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .size(width = 48.dp, height = 44.dp)
                        .testTag("power_button"),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close, 
                        contentDescription = "Shut down", 
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// Map option code to screen name
fun getMenuTitle(option: Int): String {
    return when (option) {
        1 -> "Scientific Calculator"
        2 -> "Chemistry Solver"
        3 -> "Physical Chemistry"
        4 -> "Thermodynamics"
        5 -> "Gas Laws"
        6 -> "Chemical Equilibrium"
        7 -> "Electrochemistry"
        8 -> "Chemical Kinetics"
        9 -> "Colligative Properties"
        10 -> "Periodic Table Explorer"
        11 -> "Formula Analyzer"
        12 -> "Solution Preparation"
        13 -> "Bioinformatics"
        14 -> "Unit Converter"
        15 -> "Formula Database"
        16 -> "History Log"
        17 -> "Graph Plotter"
        18 -> "Shut Down"
        else -> "Main"
    }
}

// Return colors for distinct logging rows
fun getLineColor(line: String): Color {
    return when {
        line.startsWith(">") -> Color(0xFF00FFCC)
        line.startsWith("ERROR") || line.contains("INSUFFICIENT DATA") -> Color(0xFFFF5252)
        line.startsWith("•") || line.contains("Calculated") || line.contains("SUCCESS") -> Color(0xFF9DFF70)
        line.startsWith("===") -> Color(0xFF709C76)
        else -> Color(0xFF75DC7E)
    }
}

fun getTimestamp(): String {
    return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
}

// CSV calculations log export engine
fun buildHistoryCsvFile(context: Context, list: List<HistoryRecord>): File? {
    if (list.isEmpty()) return null
    try {
        val root = context.cacheDir
        val csvFile = File(root, "biosci_casio_calculations.csv")
        val writer = FileWriter(csvFile)
        writer.append("Timestamp,Category,Details,Result\n")
        for (item in list) {
            val detailsClean = item.details.replace("\"", "\"\"")
            val ansClean = item.result.replace("\"", "\"\"")
            writer.append("${item.timestamp},\"${item.category}\",\"$detailsClean\",\"$ansClean\"\n")
        }
        writer.flush()
        writer.close()
        return csvFile
    } catch (e: Exception) {
        return null
    }
}

fun triggerFileShare(context: Context, file: File) {
    try {
        val authority = "${context.packageName}.fileprovider"
        val shareUri = FileProvider.getUriForFile(context, authority, file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, shareUri)
            putExtra(Intent.EXTRA_SUBJECT, "BIOSCI CASIO X - Offline Calculations Export")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Save Calculations CSV"))
    } catch (e: Exception) {
        Toast.makeText(context, "Error sharing file: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}


// ==========================================
// VIEW PORT MODULES IMPLEMENTATIONS
// ==========================================

// MAIN TERMINAL MENU SCREEN
@Composable
fun MainMenuListView(onOptionSelected: (Int) -> Unit) {
    val items = listOf(
        "1. Scientific Calculator" to Icons.Default.Build,
        "2. Chemistry Solver" to Icons.Default.Settings,
        "3. Physical Chemistry" to Icons.Default.Build,
        "4. Thermodynamics" to Icons.Default.Warning,
        "5. Gas Laws" to Icons.Default.Info,
        "6. Chemical Equilibrium" to Icons.Default.Refresh,
        "7. Electrochemistry" to Icons.Default.Build,
        "8. Chemical Kinetics" to Icons.Default.DateRange,
        "9. Colligative Properties" to Icons.Default.Settings,
        "10. Periodic Table Explorer" to Icons.Default.Menu,
        "11. Formula Analyzer" to Icons.Default.Check,
        "12. Solution Preparation" to Icons.Default.Info,
        "13. Bioinformatics" to Icons.Default.Search,
        "14. Unit Converter" to Icons.Default.ArrowForward,
        "15. Formula Database" to Icons.Default.Menu,
        "16. History" to Icons.Default.Check,
        "17. Graph Plotter" to Icons.Default.PlayArrow
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "SELECT SOLVER MODULE:",
                color = GeoTextWhite.copy(alpha = 0.6f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                "17 MODULES",
                color = GeoAccentBlue,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            gridItemsIndexed(items) { index, pair ->
                val isChemistryModule = index == 1 // Highlights chemical solver module (Option 2)
                
                val cardBg = if (isChemistryModule) GeoAccentBlue else GeoSurfaceSlate
                val textCol = if (isChemistryModule) GeoAccentNavy else GeoTextWhite
                val circleBg = if (isChemistryModule) Color(0xFF003355) else GeoSoftGrey
                val iconTint = if (isChemistryModule) Color.White else GeoTextWhite
                
                // Strip numbers/prefixes for premium UI look
                val cleanTitle = pair.first.replace(Regex("^\\d+\\.\\s*"), "")
                
                // Responsive display sizing & abbreviations inside balanced columns
                val displayTitle = remember(cleanTitle) {
                    when (cleanTitle) {
                        "Scientific Calculator" -> "Calculator"
                        "Chemistry Solver" -> "Chemistry"
                        "Physical Chemistry" -> "Phys Chem"
                        "Colligative Properties" -> "Colligative"
                        "Periodic Table Explorer" -> "Periodic"
                        "Formula Analyzer" -> "Analyzer"
                        "Solution Preparation" -> "Solutions"
                        "Bioinformatics" -> "Bioinfo"
                        "Unit Converter" -> "Converter"
                        "Formula Database" -> "Database"
                        "Graph Plotter" -> "Plotter"
                        else -> cleanTitle
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.95f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(cardBg)
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                        .clickable { onOptionSelected(index + 1) }
                        .padding(8.dp)
                        .testTag("menu_item_${index + 1}"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Circular icon background
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(circleBg, shape = androidx.compose.foundation.shape.CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = pair.second,
                                contentDescription = "",
                                tint = iconTint,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(7.dp))
                        
                        Text(
                            text = displayTitle,
                            color = textCol,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 12.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }
            }
        }
    }
}

// 1. SCIENTIFIC CALCULATOR
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScientificCalculatorUi(
    terminalStream: MutableList<String>,
    historySave: (String, String) -> Unit
) {
    var txtVal by remember { mutableStateOf("") }
    var calcResult by remember { mutableStateOf("") }

    val keypadShortcuts = listOf(
        "sin", "cos", "tan", "log", "ln", "sqrt", "^", "!", "e", "pi", "(", ")"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Text("SCIENTIFIC CALCULATOR (SymPy-Like Expression Parser)", color = Color(0xFFF39C12), fontSize = 11.sp)
        Spacer(modifier = Modifier.height(4.dp))
        
        OutlinedTextField(
            value = txtVal,
            onValueChange = { txtVal = it },
            modifier = Modifier.fillMaxWidth().testTag("calc_expression_field"),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF76C786),
                unfocusedBorderColor = Color.Gray
            ),
            placeholder = { Text("(5+8)*12/3 or sin(pi/4)^2", color = Color.Gray, fontSize = 12.sp) }
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Grid-like dynamic scientific keys row
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            maxItemsInEachRow = 4
        ) {
            keypadShortcuts.forEach { key ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF32323D))
                        .clickable {
                            txtVal += if (key in listOf("e", "pi", ")")) key else "$key("
                        }
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(key, color = Color(0xFFA2A2B1), fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = { txtVal = ""; calcResult = "" },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5A5A66)),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("CLEAR", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }

            Button(
                onClick = {
                    try {
                        val solver = MathParser(txtVal)
                        val ans = solver.parse()
                        calcResult = String.format(Locale.getDefault(), "%.6g", ans)
                        terminalStream.add("======================")
                        terminalStream.add("EXPR: $txtVal")
                        terminalStream.add("• RESULT = $calcResult")
                        terminalStream.add("======================")
                        historySave(txtVal, calcResult)
                    } catch (e: Exception) {
                        calcResult = "ERROR: ${e.message}"
                        terminalStream.add("• Calc Exception: ${e.message}")
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E824C)),
                modifier = Modifier.weight(1.5f).testTag("calc_evaluate_button"),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("SOLVE EXP", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        }

        if (calcResult.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text("Result: $calcResult", color = if (calcResult.contains("ERROR")) Color.Red else Color(0xFF9EFFAC), fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
    }
}

// 2. CHEMISTRY SOLVER
@Composable
fun ChemistrySolverUi(
    terminalStream: MutableList<String>,
    historySave: (String, String) -> Unit
) {
    val varsToFind = listOf(
        ChemVar.MOLARITY, ChemVar.MOLES, ChemVar.MASS, ChemVar.VOLUME,
        ChemVar.NORMALITY, ChemVar.PH, ChemVar.POH, ChemVar.STRENGTH, ChemVar.PPM, ChemVar.PPB
    )

    var targetVar by remember { mutableStateOf(ChemVar.MOLARITY) }
    var selectedFormulaStr by remember { mutableStateOf("") }
    
    // User inputs
    var ipMass by remember { mutableStateOf("") }
    var ipMw by remember { mutableStateOf("") }
    var ipVol by remember { mutableStateOf("") }
    var ipMoles by remember { mutableStateOf("") }
    var ipNFactor by remember { mutableStateOf("") }
    var ipNormality by remember { mutableStateOf("") }
    var ipPh by remember { mutableStateOf("") }
    var ipStrength by remember { mutableStateOf("") }

    val solver = remember { ChemistrySolverEngine() }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text("AUTOMATED MULTI-ROUTE CHEMISTRY PATHFINDER", color = Color(0xFFE67E22), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
            Text("Find: ", color = Color.White, fontSize = 11.sp, modifier = Modifier.width(36.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                varsToFind.forEach { v ->
                    val sel = targetVar == v
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (sel) Color(0xFFE67E22) else Color(0xFF32323D))
                            .clickable { targetVar = v }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(v.name, color = if (sel) Color.Black else Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        Text("Enter ANY known values in fields below (Engine automatically chooses route):", color = Color.Gray, fontSize = 10.sp)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedTextField(
                value = ipMass, onValueChange = { ipMass = it },
                label = { Text("Mass (g)", fontSize = 9.sp) },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                modifier = Modifier.weight(1f).height(46.dp),
                textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            )
            OutlinedTextField(
                value = ipMw, onValueChange = { ipMw = it },
                label = { Text("Mw (g/mol)", fontSize = 9.sp) },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                modifier = Modifier.weight(1f).height(46.dp),
                textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp).also { Spacer(modifier = Modifier.height(2.dp)) }) {
            OutlinedTextField(
                value = ipVol, onValueChange = { ipVol = it },
                label = { Text("Vol (L)", fontSize = 9.sp) },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                modifier = Modifier.weight(1f).height(46.dp),
                textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            )
            OutlinedTextField(
                value = ipMoles, onValueChange = { ipMoles = it },
                label = { Text("Moles", fontSize = 9.sp) },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                modifier = Modifier.weight(1f).height(46.dp),
                textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedTextField(
                value = ipNFactor, onValueChange = { ipNFactor = it },
                label = { Text("n-factor", fontSize = 9.sp) },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                modifier = Modifier.weight(1f).height(46.dp),
                textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            )
            OutlinedTextField(
                value = ipNormality, onValueChange = { ipNormality = it },
                label = { Text("Normality (N)", fontSize = 9.sp) },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                modifier = Modifier.weight(1f).height(46.dp),
                textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedTextField(
                value = ipPh, onValueChange = { ipPh = it },
                label = { Text("pH Level", fontSize = 9.sp) },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                modifier = Modifier.weight(1f).height(46.dp),
                textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            )
            OutlinedTextField(
                value = ipStrength, onValueChange = { ipStrength = it },
                label = { Text("Strength (g/L)", fontSize = 9.sp) },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                modifier = Modifier.weight(1f).height(46.dp),
                textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Button(
            onClick = {
                val inputMap = mutableMapOf<ChemVar, Double>()
                ipMass.toDoubleOrNull()?.let { inputMap[ChemVar.MASS] = it }
                ipMw.toDoubleOrNull()?.let { inputMap[ChemVar.MOL_WEIGHT] = it }
                ipVol.toDoubleOrNull()?.let { inputMap[ChemVar.VOLUME] = it }
                ipMoles.toDoubleOrNull()?.let { inputMap[ChemVar.MOLES] = it }
                ipNFactor.toDoubleOrNull()?.let { inputMap[ChemVar.N_FACTOR] = it }
                ipNormality.toDoubleOrNull()?.let { inputMap[ChemVar.NORMALITY] = it }
                ipPh.toDoubleOrNull()?.let { inputMap[ChemVar.PH] = it }
                ipStrength.toDoubleOrNull()?.let { inputMap[ChemVar.STRENGTH] = it }

                val logsList = mutableListOf<String>()
                logsList.add("=========================================")
                logsList.add("CHEMISTRY AUTO SOLVER ROUTING START:")
                logsList.add("• Target variable: ${targetVar.displayName}")
                
                val ans = solver.solve(targetVar, inputMap, logsList)
                if (ans != null) {
                    logsList.add("-----------------------------------------")
                    logsList.add("SUCCESS!! UNKNOWN SOLVED:")
                    logsList.add("• ${targetVar.displayName} = ${String.format("%.6g", ans)} ${targetVar.unit}")
                    logsList.add("=========================================")
                    
                    historySave("Solved ${targetVar.displayName}", "${String.format("%.4f", ans)} ${targetVar.unit}")
                } else {
                    logsList.add("-----------------------------------------")
                    logsList.add("INSUFFICIENT DATA")
                    logsList.add("=========================================")
                }
                terminalStream.addAll(logsList)
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD35400)),
            modifier = Modifier.fillMaxWidth().testTag("chem_solver_button"),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text("RESOLVE TARGET CHEMISTRY VALUE", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

// 3. PHYSICAL CHEMISTRY
@Composable
fun PhysicalChemistryUi(
    terminalStream: MutableList<String>,
    historySave: (String, String) -> Unit
) {
    var ionicConcentration by remember { mutableStateOf("") }
    var chargeVal by remember { mutableStateOf("") }
    var ansStr by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("PHYSICAL CHEMISTRY ENGINE: Ionic Strength Solver", color = Color(0xFF1ABC9C), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text("Formula: I = 0.5 * Sum( C_i * Z_i^2 )", color = Color.Gray, fontSize = 9.sp)
        
        OutlinedTextField(
            value = ionicConcentration, onValueChange = { ionicConcentration = it },
            label = { Text("Species Concentration (M)", fontSize = 11.sp) },
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
            modifier = Modifier.fillMaxWidth().height(48.dp),
            textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = chargeVal, onValueChange = { chargeVal = it },
            label = { Text("Species Valuation Charge (Z)", fontSize = 11.sp) },
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
            modifier = Modifier.fillMaxWidth().height(48.dp),
            textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Button(
            onClick = {
                val conc = ionicConcentration.toDoubleOrNull()
                val charge = chargeVal.toDoubleOrNull()
                if (conc != null && charge != null) {
                    val strength = 0.5 * conc * (charge * charge)
                    ansStr = String.format("%.5f M", strength)
                    terminalStream.add("• Ionic Strength Resolved: [Conc=$conc, Z=$charge] -> I = $ansStr")
                    historySave("Ionic Strength calculation", ansStr)
                } else {
                    ansStr = "INSUFFICIENT DATA"
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1ABC9C)),
            modifier = Modifier.fillMaxWidth().testTag("physchem_solve_button"),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text("CALCULATE IONIC STRENGTH", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }

        if (ansStr.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text("Result: $ansStr", color = Color(0xFF1ABC9C), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
    }
}

// 4. THERMODYNAMICS
@Composable
fun ThermodynamicsUi(
    terminalStream: MutableList<String>,
    historySave: (String, String) -> Unit
) {
    var dH by remember { mutableStateOf("") }
    var dS by remember { mutableStateOf("") }
    var temp by remember { mutableStateOf("298.15") } // Default Kelvin standard
    var dGResult by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("THERMODYNAMICS ENGINE (Gibbs Free Energy)", color = Color(0xFFE74C3C), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text("Formula: dG = dH - T * dS", color = Color.Gray, fontSize = 9.sp)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedTextField(
                value = dH, onValueChange = { dH = it },
                label = { Text("dH (kJ/mol)", fontSize = 10.sp) },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                modifier = Modifier.weight(1f).height(46.dp),
                textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            )
            OutlinedTextField(
                value = dS, onValueChange = { dS = it },
                label = { Text("dS (J/mol*K)", fontSize = 10.sp) },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                modifier = Modifier.weight(1f).height(46.dp),
                textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        OutlinedTextField(
            value = temp, onValueChange = { temp = it },
            label = { Text("Temperature (Kelvin)", fontSize = 10.sp) },
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
            modifier = Modifier.fillMaxWidth().height(46.dp),
            textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Button(
            onClick = {
                val h = dH.toDoubleOrNull()
                val s = dS.toDoubleOrNull() // Needs to be converted to kJ for equation consistency
                val t = temp.toDoubleOrNull()

                if (h != null && s != null && t != null) {
                    val sKj = s / 1000.0
                    val g = h - (t * sKj)
                    val spontaneity = if (g < 0) "SPONTANEOUS ENERGETICALLY" else if (g > 0) "NON-SPONTANEOUS" else "SYSTEM IN EQUILIBRIUM"
                    dGResult = "dG = ${String.format("%.4f", g)} kJ/mol ($spontaneity)"
                    
                    terminalStream.add("======================")
                    terminalStream.add("THERMO SPECS:")
                    terminalStream.add("• dH: $h kJ/mol | dS: $s J/mol*K | T: $t K")
                    terminalStream.add("• Resolved dG: ${String.format("%.4f", g)} kJ/mol")
                    terminalStream.add("• Outcome: $spontaneity")
                    terminalStream.add("======================")
                    historySave("Gibbs Free Energy", dGResult)
                } else {
                    dGResult = "INSUFFICIENT DATA"
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE74C3C)),
            modifier = Modifier.fillMaxWidth().testTag("thermo_solve_button"),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text("SOLVE GIBBS ENERGETICS", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }

        if (dGResult.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(dGResult, color = Color(0xFFFF9E9E), fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }
    }
}

// 5. GAS LAWS
@Composable
fun GasLawsUi(
    terminalStream: MutableList<String>,
    historySave: (String, String) -> Unit
) {
    val gasUnits = listOf("Ideal Gas PV=nRT", "Charles V1/T1=V2/T2", "Boyle P1V1=P2V2", "Gay-Lussac P1/T1=P2/T2")
    var selectedLawIdx by remember { mutableStateOf(0) }

    var p1 by remember { mutableStateOf("") }
    var v1 by remember { mutableStateOf("") }
    var n1 by remember { mutableStateOf("") }
    var t1 by remember { mutableStateOf("") }
    var outcomeGas by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("GAS SYSTEMS MATRIX CODES", color = Color(0xFF3498DB), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            gasUnits.forEachIndexed { idx, title ->
                val active = selectedLawIdx == idx
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (active) Color(0xFF3498DB) else Color(0xFF32323D))
                        .clickable { selectedLawIdx = idx; outcomeGas = "" }
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(title, color = if (active) Color.Black else Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        Text("Select unknown by leaving that field completely BLANK:", color = Color.Gray, fontSize = 9.sp)

        if (selectedLawIdx == 0) {
            // PV = nRT
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = p1, onValueChange = { p1 = it }, label = { Text("P1 (atm)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                    modifier = Modifier.weight(1f).height(46.dp), textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                )
                OutlinedTextField(
                    value = v1, onValueChange = { v1 = it }, label = { Text("V1 (L)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                    modifier = Modifier.weight(1f).height(46.dp), textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = n1, onValueChange = { n1 = it }, label = { Text("n (moles)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                    modifier = Modifier.weight(1f).height(46.dp), textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                )
                OutlinedTextField(
                    value = t1, onValueChange = { t1 = it }, label = { Text("T (Kelvin)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                    modifier = Modifier.weight(1f).height(46.dp), textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                )
            }
        } else {
            // Boyle, Charles etc. two-state variables
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = p1, onValueChange = { p1 = it }, label = { Text("Initial (P1/V1)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                    modifier = Modifier.weight(1f).height(46.dp), textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                )
                OutlinedTextField(
                    value = v1, onValueChange = { v1 = it }, label = { Text("Initial Temp/Vol (T1)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                    modifier = Modifier.weight(1f).height(46.dp), textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = n1, onValueChange = { n1 = it }, label = { Text("Target (P2/V2)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                    modifier = Modifier.weight(1f).height(46.dp), textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                )
                OutlinedTextField(
                    value = t1, onValueChange = { t1 = it }, label = { Text("Target Temp (T2)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                    modifier = Modifier.weight(1f).height(46.dp), textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Button(
            onClick = {
                val r = 0.082057 // Gas constant
                if (selectedLawIdx == 0) {
                    val pVal = p1.toDoubleOrNull()
                    val vVal = v1.toDoubleOrNull()
                    val nVal = n1.toDoubleOrNull()
                    val tVal = t1.toDoubleOrNull()

                    val emptyCount = listOf(pVal, vVal, nVal, tVal).count { it == null }
                    if (emptyCount == 1) {
                        if (pVal == null) {
                            val solvedP = (nVal!! * r * tVal!!) / vVal!!
                            outcomeGas = "Solved P = ${String.format("%.4f", solvedP)} atm"
                        } else if (vVal == null) {
                            val solvedV = (nVal!! * r * tVal!!) / pVal
                            outcomeGas = "Solved V = ${String.format("%.4f", solvedV)} L"
                        } else if (nVal == null) {
                            val solvedN = (pVal * vVal!!) / (r * tVal!!)
                            outcomeGas = "Solved n = ${String.format("%.4f", solvedN)} moles"
                        } else {
                            val solvedT = (pVal * vVal!!) / (nVal * r)
                            outcomeGas = "Solved T = ${String.format("%.4f", solvedT)} K"
                        }
                        terminalStream.add("• Gas Solver SUCCESS: $outcomeGas")
                        historySave(gasUnits[selectedLawIdx], outcomeGas)
                    } else {
                        outcomeGas = "INSUFFICIENT DATA"
                    }
                } else if (selectedLawIdx == 2) {
                    // Boyle's Law: P1*V1 = P2*V2
                    val valP1 = p1.toDoubleOrNull()
                    val valV1 = v1.toDoubleOrNull()
                    val valP2 = n1.toDoubleOrNull()
                    val valV2 = t1.toDoubleOrNull()
                    val nullCount = listOf(valP1, valV1, valP2, valV2).count { it == null }
                    if (nullCount == 1) {
                        if (valP1 == null) {
                            outcomeGas = "Solved P1 = ${String.format("%.4f", (valP2!! * valV2!!) / valV1!!)} units"
                        } else if (valV1 == null) {
                            outcomeGas = "Solved V1 = ${String.format("%.4f", (valP2!! * valV2!!) / valP1)} units"
                        } else if (valP2 == null) {
                            outcomeGas = "Solved P2 = ${String.format("%.4f", (valP1 * valV1) / valV2!!)} units"
                        } else {
                            outcomeGas = "Solved V2 = ${String.format("%.4f", (valP1 * valV1) / valP2)} units"
                        }
                        terminalStream.add("• Boyle's Law Solver Success: $outcomeGas")
                        historySave(gasUnits[selectedLawIdx], outcomeGas)
                    } else {
                        outcomeGas = "INSUFFICIENT DATA"
                    }
                } else {
                    // Simulated response for Charles / Gay-Lussac
                    val ip1 = p1.toDoubleOrNull()
                    val it1 = v1.toDoubleOrNull()
                    val ip2 = n1.toDoubleOrNull()
                    val it2 = t1.toDoubleOrNull()
                    if (listOf(ip1, it1, ip2, it2).count { it == null } == 1) {
                        if (ip1 == null) {
                            outcomeGas = "Solved Val = ${String.format("%.4f", (ip2!! * it1!!) / it2!!)}"
                        } else if (it1 == null) {
                            outcomeGas = "Solved Val = ${String.format("%.4f", (ip1 * it2!!) / ip2!!)}"
                        } else if (ip2 == null) {
                            outcomeGas = "Solved Val = ${String.format("%.4f", (ip1 * it2!!) / it1)}"
                        } else {
                            outcomeGas = "Solved Val = ${String.format("%.4f", (ip2 * it1) / ip1)}"
                        }
                        terminalStream.add("• Gas Law Linear ratio Resolved: $outcomeGas")
                        historySave(gasUnits[selectedLawIdx], outcomeGas)
                    } else {
                        outcomeGas = "INSUFFICIENT DATA"
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3498DB)),
            modifier = Modifier.fillMaxWidth().testTag("gas_solve_button"),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text("SOLVE SPECIFIED GAS PROPERTY", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }

        if (outcomeGas.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(outcomeGas, color = Color(0xFF91D8F7), fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }
    }
}

// 6. CHEMICAL EQUILIBRIUM
@Composable
fun ChemicalEquilibriumUi(
    terminalStream: MutableList<String>,
    historySave: (String, String) -> Unit
) {
    var kcInput by remember { mutableStateOf("") }
    var deltaNInput by remember { mutableStateOf("") }
    var tempEquilInput by remember { mutableStateOf("298.15") }
    var ansEquil by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("CHEMICAL EQUILIBRIUM COUPLING (Kp & Kc relation)", color = Color(0xFF9B59B6), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text("Formula: Kp = Kc * (R * T)^dn [R = 0.08206]", color = Color.Gray, fontSize = 9.sp)

        OutlinedTextField(
            value = kcInput, onValueChange = { kcInput = it },
            label = { Text("Equilibrium constant (Kc)", fontSize = 11.sp) },
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
            modifier = Modifier.fillMaxWidth().height(46.dp), textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        )
        Spacer(modifier = Modifier.height(2.dp))
        OutlinedTextField(
            value = deltaNInput, onValueChange = { deltaNInput = it },
            label = { Text("Delta n (moles of product gas - reactant gas)", fontSize = 11.sp) },
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
            modifier = Modifier.fillMaxWidth().height(46.dp), textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        )
        Spacer(modifier = Modifier.height(2.dp))
        OutlinedTextField(
            value = tempEquilInput, onValueChange = { tempEquilInput = it },
            label = { Text("Temperature (K)", fontSize = 11.sp) },
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
            modifier = Modifier.fillMaxWidth().height(46.dp), textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Button(
            onClick = {
                val kc = kcInput.toDoubleOrNull()
                val dn = deltaNInput.toDoubleOrNull()
                val temp = tempEquilInput.toDoubleOrNull()
                if (kc != null && dn != null && temp != null) {
                    val kp = kc * (0.082057 * temp).pow(dn)
                    ansEquil = "Kp = ${String.format("%.5e", kp)}"
                    terminalStream.add("• Equilibrium Resolved: [Kc=$kc, dn=$dn, T=$temp] -> Kp = $ansEquil")
                    historySave("Kp calculation from Kc", ansEquil)
                } else {
                    ansEquil = "INSUFFICIENT DATA"
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9B59B6)),
            modifier = Modifier.fillMaxWidth().testTag("equil_solve_button"),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text("CALCULATE Kp EQUILIBRIUM", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }

        if (ansEquil.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text("Result: $ansEquil", color = Color(0xFFD499E2), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
    }
}

// 7. ELECTROCHEMISTRY
@Composable
fun ElectrochemistryUi(
    terminalStream: MutableList<String>,
    historySave: (String, String) -> Unit
) {
    var stdPotential by remember { mutableStateOf("") }
    var electronsN by remember { mutableStateOf("") }
    var quotientQ by remember { mutableStateOf("") }
    var potResult by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("ELECTROCHEMISTRY NERNST POTENTIAL", color = Color(0xFFF1C40F), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text("Equation: E = Eo - (0.0592 / n) * log10(Q)", color = Color.Gray, fontSize = 9.sp)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedTextField(
                value = stdPotential, onValueChange = { stdPotential = it },
                label = { Text("Standard Eo (V)", fontSize = 10.sp) },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                modifier = Modifier.weight(1f).height(46.dp), textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            )
            OutlinedTextField(
                value = electronsN, onValueChange = { electronsN = it },
                label = { Text("electrons (n)", fontSize = 10.sp) },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                modifier = Modifier.weight(1f).height(46.dp), textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        OutlinedTextField(
            value = quotientQ, onValueChange = { quotientQ = it },
            label = { Text("Reaction Quotient (Q)", fontSize = 11.sp) },
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
            modifier = Modifier.fillMaxWidth().height(46.dp), textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Button(
            onClick = {
                val eo = stdPotential.toDoubleOrNull()
                val n = electronsN.toDoubleOrNull()
                val q = quotientQ.toDoubleOrNull()
                if (eo != null && n != null && q != null && q > 0.0) {
                    val cellPot = eo - (0.0592 / n) * log10(q)
                    potResult = "${String.format("%.4f", cellPot)} Volts"
                    terminalStream.add("• Nernst Cell E Calculated: Std=$eo V, n=$n, Q=$q -> Cell E = $potResult")
                    historySave("Nernst Cell Electrochemistry", potResult)
                } else {
                    potResult = "INSUFFICIENT DATA"
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD6A20E)),
            modifier = Modifier.fillMaxWidth().testTag("electro_solve_button"),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text("CALCULATE EXPLICIT CELL POTENTIAL", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }

        if (potResult.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text("Result E = $potResult", color = Color(0xFFFFF490), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
    }
}

// 8. CHEMICAL KINETICS
@Composable
fun ChemicalKineticsUi(
    terminalStream: MutableList<String>,
    historySave: (String, String) -> Unit
) {
    var textA0 by remember { mutableStateOf("") }
    var textAt by remember { mutableStateOf("") }
    var textTime by remember { mutableStateOf("") }
    var ansKinetics by remember { mutableStateOf("") }

    var selectedOrder by remember { mutableStateOf(1) } // 0=Zero, 1=First, 2=Second Order

    Column(modifier = Modifier.fillMaxSize()) {
        Text("CHEMICAL KINETICS INTEGRATED RATE EXPR", color = Color(0xFF16A085), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val orders = listOf("Zero Order", "First Order", "Second Order")
            orders.forEachIndexed { idx, text ->
                val chosen = selectedOrder == idx
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (chosen) Color(0xFF16A085) else Color(0xFF32323D))
                        .clickable { selectedOrder = idx; ansKinetics = "" }
                        .padding(vertical = 4.dp), contentAlignment = Alignment.Center
                ) {
                    Text(text, color = if (chosen) Color.Black else Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedTextField(
                value = textA0, onValueChange = { textA0 = it }, label = { Text("Initial [A]0", fontSize = 10.sp) },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                modifier = Modifier.weight(1f).height(46.dp), textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            )
            OutlinedTextField(
                value = textAt, onValueChange = { textAt = it }, label = { Text("Final [A]t", fontSize = 10.sp) },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                modifier = Modifier.weight(1f).height(46.dp), textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        OutlinedTextField(
            value = textTime, onValueChange = { textTime = it }, label = { Text("Time Elapsed (seconds/mins)", fontSize = 11.sp) },
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
            modifier = Modifier.fillMaxWidth().height(46.dp), textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Button(
            onClick = {
                val a0 = textA0.toDoubleOrNull()
                val at = textAt.toDoubleOrNull()
                val t = textTime.toDoubleOrNull()
                if (a0 != null && at != null && t != null && t > 0.0) {
                    when (selectedOrder) {
                        0 -> {
                            val k = (a0 - at) / t
                            val halfLife = a0 / (2 * k)
                            ansKinetics = "k = ${String.format("%.5g", k)} (M/s) [Half-Life = ${String.format("%.5g", halfLife)} s]"
                        }
                        1 -> {
                            if (at > 0.0 && a0 > 0.0) {
                                val k = ln(a0 / at) / t
                                val halfLife = ln(2.0) / k
                                ansKinetics = "k = ${String.format("%.5g", k)} (1/s) [Half-Life = ${String.format("%.5g", halfLife)} s]"
                            } else {
                                ansKinetics = "INSUFFICIENT DATA (A values must be positive)"
                            }
                        }
                        2 -> {
                            if (at > 0.0 && a0 > 0.0) {
                                val k = (1.0 / at - 1.0 / a0) / t
                                val halfLife = 1.0 / (k * a0)
                                ansKinetics = "k = ${String.format("%.5g", k)} (1/M*s) [Half-Life = ${String.format("%.5g", halfLife)} s]"
                            } else {
                                ansKinetics = "INSUFFICIENT DATA"
                            }
                        }
                    }
                    terminalStream.add("• Kinetics Rate Constant Resolved: $ansKinetics")
                    historySave("Chemical Kinetics Order $selectedOrder", ansKinetics)
                } else {
                    ansKinetics = "INSUFFICIENT DATA"
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A085)),
            modifier = Modifier.fillMaxWidth().testTag("kinetics_solve_button"),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text("RESOLVE RATE & HALF-LIFE", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }

        if (ansKinetics.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(ansKinetics, color = Color(0xFF86E2D5), fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }
    }
}

// 9. COLLIGATIVE PROPERTIES
@Composable
fun ColligativePropertiesUi(
    terminalStream: MutableList<String>,
    historySave: (String, String) -> Unit
) {
    var vantHoffInput by remember { mutableStateOf("1") }
    var thermalConstInput by remember { mutableStateOf("1.86") } // Default water cryoscopic Kf
    var soluteMolality by remember { mutableStateOf("") }
    var dpResult by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("COLLIGATIVE DEPRESSION/ELEVATION", color = Color(0xFF34495E), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text("Formula: dT = i * K * molality", color = Color.Gray, fontSize = 9.sp)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedTextField(
                value = vantHoffInput, onValueChange = { vantHoffInput = it },
                label = { Text("Hoff i factor", fontSize = 10.sp) },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                modifier = Modifier.weight(1f).height(46.dp), textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            )
            OutlinedTextField(
                value = thermalConstInput, onValueChange = { thermalConstInput = it },
                label = { Text("Const K (Kf/Kb)", fontSize = 10.sp) },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                modifier = Modifier.weight(1f).height(46.dp), textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        OutlinedTextField(
            value = soluteMolality, onValueChange = { soluteMolality = it },
            label = { Text("Solute molality (mol/kg)", fontSize = 11.sp) },
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
            modifier = Modifier.fillMaxWidth().height(46.dp), textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Button(
            onClick = {
                val i = vantHoffInput.toDoubleOrNull()
                val k = thermalConstInput.toDoubleOrNull()
                val m = soluteMolality.toDoubleOrNull()
                if (i != null && k != null && m != null) {
                    val dT = i * k * m
                    dpResult = "dT = ${String.format("%.4f", dT)} Kelvin deviation"
                    terminalStream.add("• Colligative property Resolved: i=$i, K=$k, m=$m -> dT = $dT K")
                    historySave("Colligative Temp Shift", dpResult)
                } else {
                    dpResult = "INSUFFICIENT DATA"
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34495E)),
            modifier = Modifier.fillMaxWidth().testTag("colligative_solve_button"),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text("SOLVE COLLIGATIVE SHIFT (dT)", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }

        if (dpResult.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(dpResult, color = Color(0xFFAABBCC), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        }
    }
}

// 10. PERIODIC TABLE EXPLORER
@Composable
fun PeriodicTableExplorerUi() {
    var searchKey by remember { mutableStateOf("") }
    var selectedElement by remember { mutableStateOf<Element?>(PeriodicTable.elementsList[0]) }

    val filteredList = remember(searchKey) {
        if (searchKey.isEmpty()) {
            PeriodicTable.elementsList
        } else {
            PeriodicTable.elementsList.filter {
                it.name.contains(searchKey, ignoreCase = true) ||
                        it.symbol.contains(searchKey, ignoreCase = true) ||
                        it.number.toString() == searchKey
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("OFFLINE PERIODIC ELEMENT PROFILE EXPLORER", color = Color(0xFF2ECC71), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        
        OutlinedTextField(
            value = searchKey, onValueChange = { searchKey = it },
            placeholder = { Text("Search Symbol / Atomic # / Name", color = Color.Gray, fontSize = 10.sp) },
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
            modifier = Modifier.fillMaxWidth().height(42.dp),
            textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Left list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .border(1.dp, Color(0xFF32323D), RoundedCornerShape(4.dp)),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(filteredList) { elem ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (selectedElement == elem) Color(0xFF2E7D32) else Color.Transparent)
                            .clickable { selectedElement = elem }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Text("${elem.number}. ${elem.symbol} - ${elem.name}", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            // Right Detail Profile Drawer card
            selectedElement?.let { elem ->
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .background(Color(0xFF26262F), RoundedCornerShape(6.dp))
                        .padding(8.dp)
                ) {
                    Text(elem.name.uppercase(), fontSize = 12.sp, color = Color(0xFF4EEF80), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Symbol: ${elem.symbol}", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Text("No: ${elem.number}", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                    Text("Mass: ${elem.mass} u", color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    Text("Period: ${elem.period} | Group: ${elem.group}", color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    Text("State (STP): ${elem.stateAtStp}", color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    Text("Electronegativity: ${elem.electroNegativity ?: "N/A"}", color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    
                    Spacer(modifier = Modifier.height(2.dp))
                    Divider(color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    Text("Atomics Structure Ratio:", color = Color.Gray, fontSize = 8.sp)
                    Text("• Protons: ${elem.number}", color = Color(0xFFF39C12), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    Text("• Electrons: ${elem.number}", color = Color(0xFF3498DB), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    val neutrons = (elem.mass - elem.number).roundToInt()
                    Text("• Neutrons: $neutrons", color = Color(0xFF9B59B6), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

// 11. FORMULA ANALYZER
@Composable
fun ChemicalFormulaAnalyzerUi(
    terminalStream: MutableList<String>,
    historySave: (String, String) -> Unit
) {
    var queryFormula by remember { mutableStateOf("C6H12O6") }
    var resultsText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("MOLECULAR CHEMICAL FORMULA BREAKDOWN", color = Color(0xFF9B59B6), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text("Validates syntax containing subgroups like Ca(OH)2 correctly:", color = Color.Gray, fontSize = 9.sp)

        OutlinedTextField(
            value = queryFormula, onValueChange = { queryFormula = it },
            placeholder = { Text("e.g. Al2(SO4)3 or H2SO4", color = Color.Gray) },
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
            modifier = Modifier.fillMaxWidth().height(48.dp),
            textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Button(
            onClick = {
                val elementCounts = parseFormula(queryFormula)
                if (elementCounts.isEmpty()) {
                    resultsText = "ERROR: INVALID FORMULA SYNTAX"
                } else {
                    val mw = calculateMolecularMass(queryFormula)
                    val builder = StringBuilder()
                    builder.append("Formula: $queryFormula\n")
                    builder.append("• Molecular Weight: ${String.format("%.4f", mw)} g/mol\n")
                    builder.append("Elemental Composition Percent:\n")
                    
                    terminalStream.add("======================")
                    terminalStream.add("FORMULA: $queryFormula")
                    terminalStream.add("• Total Mw: ${String.format("%.4f", mw)} g/mol")
                    
                    elementCounts.forEach { (elem, count) ->
                        val atomicWeight = PeriodicTable.findSymbol(elem)?.mass ?: 0.0
                        val elementalMass = atomicWeight * count
                        val pct = if (mw > 0.0) (elementalMass / mw) * 100.0 else 0.0
                        val rowStr = " • $elem: ${count.toInt()} atoms (${String.format("%.2f", pct)}%)"
                        builder.append(rowStr).append("\n")
                        terminalStream.add(rowStr)
                    }
                    terminalStream.add("======================")
                    resultsText = builder.toString()
                    historySave("Analyzed Formula $queryFormula", "${String.format("%.2f", mw)} g/mol")
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E44AD)),
            modifier = Modifier.fillMaxWidth().testTag("formula_analyze_button"),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text("ANALYZE STOICHIOMETRY COMPOSITION", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }

        if (resultsText.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .background(Color(0xFF26262F), RoundedCornerShape(4.dp))
                    .border(1.dp, Color.DarkGray)
                    .padding(6.dp)
            ) {
                Text(resultsText, color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

// 12. SOLUTION PREPARATION
@Composable
fun SolutionPreparationUi(
    terminalStream: MutableList<String>,
    historySave: (String, String) -> Unit
) {
    var compoundFormula by remember { mutableStateOf("NaOH") }
    var targetMolarity by remember { mutableStateOf("0.5") }
    var targetVolumeMl by remember { mutableStateOf("250") }
    var preparationGuide by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text("CHEMISTRY SOLUTION PREPARATION STEPS MODEL", color = Color(0xFFE67E22), fontSize = 11.sp, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = compoundFormula, onValueChange = { compoundFormula = it },
            label = { Text("Compound Formula", fontSize = 10.sp) },
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
            modifier = Modifier.fillMaxWidth().height(46.dp), textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedTextField(
                value = targetMolarity, onValueChange = { targetMolarity = it },
                label = { Text("Target Conc (M)", fontSize = 10.sp) },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                modifier = Modifier.weight(1f).height(46.dp), textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            )
            OutlinedTextField(
                value = targetVolumeMl, onValueChange = { targetVolumeMl = it },
                label = { Text("Vol (mL)", fontSize = 10.sp) },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                modifier = Modifier.weight(1f).height(46.dp), textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Button(
            onClick = {
                val mw = calculateMolecularMass(compoundFormula)
                val molarity = targetMolarity.toDoubleOrNull()
                val volMl = targetVolumeMl.toDoubleOrNull()

                if (mw > 0.0 && molarity != null && volMl != null) {
                    val volLiters = volMl / 1000.0
                    val reqMass = molarity * volLiters * mw

                    preparationGuide = "=== PROTOCOL COMPILATION ===\n" +
                            "To prepare $volMl mL of $molarity M $compoundFormula:\n" +
                            "1. Molecular weight is ${String.format("%.3f", mw)} g/mol.\n" +
                            "2. Weigh out exactly ${String.format("%.4f", reqMass)} g of solid $compoundFormula.\n" +
                            "3. Dissolve in ~ ${String.format("%.0f", volMl * 0.7)} mL deionized water.\n" +
                            "4. Adjust total final volume to exactly $volMl mL.\n" +
                            "5. Stir well till homogenous."

                    terminalStream.add("======================")
                    terminalStream.add("PREP COMPLETED:")
                    terminalStream.add("• For: $volMl mL of $molarity M $compoundFormula")
                    terminalStream.add("• Required Mass: ${String.format("%.4f", reqMass)} g")
                    terminalStream.add("======================")
                    historySave("Solution Prep for $compoundFormula", "${String.format("%.2f", reqMass)} g")
                } else {
                    preparationGuide = "INSUFFICIENT DATA / ELEMENT INVALID"
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD35400)),
            modifier = Modifier.fillMaxWidth().testTag("solution_prep_button"),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text("GENERATE LABORATORY PROTOCOL", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }

        if (preparationGuide.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .background(Color(0xFF26262F), RoundedCornerShape(4.dp))
                    .padding(6.dp)
            ) {
                Text(preparationGuide, color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

// 13. BIOINFORMATICS
@Composable
fun BioinformaticsUi(
    terminalStream: MutableList<String>,
    historySave: (String, String) -> Unit
) {
    val optionsList = listOf("Transcribe DNA", "Translate RNA", "GC Content", "Reverse Complement", "Protein MW")
    var selectedOpt by remember { mutableStateOf(0) }
    var inputSeq by remember { mutableStateOf("ATGCATGCGCTAG") }
    var bioResult by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("OFFLINE MOLECULAR BIOINFORMATICS PIPELINE", color = Color(0xFF1ABC9C), fontSize = 10.sp, fontWeight = FontWeight.Bold)

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            optionsList.forEachIndexed { i, label ->
                val chosen = selectedOpt == i
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (chosen) Color(0xFF1ABC9C) else Color(0xFF32323D))
                        .clickable { selectedOpt = i; bioResult = "" }
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(label, color = if (chosen) Color.Black else Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        OutlinedTextField(
            value = inputSeq, onValueChange = { inputSeq = it },
            label = { Text("Input Sequence (Nucleic Acids / Amino Residues)", fontSize = 10.sp) },
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
            modifier = Modifier.fillMaxWidth().height(46.dp), textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Button(
            onClick = {
                val up = inputSeq.trim().uppercase()
                if (up.isEmpty()) {
                    bioResult = "ERROR: SEQUENCE IS EMPTY"
                    return@Button
                }
                bioResult = when (selectedOpt) {
                    0 -> BioinformaticsEngine.transcribeDnaToRna(up)
                    1 -> BioinformaticsEngine.translateRnaToProtein(up)
                    2 -> {
                        val gc = BioinformaticsEngine.calculateGcContent(up)
                        "${String.format("%.3f", gc)}%"
                    }
                    3 -> BioinformaticsEngine.reverseComplement(up)
                    4 -> {
                        val mw = BioinformaticsEngine.calculateProteinMw(up)
                        "${String.format("%.3f", mw)} g/mol"
                    }
                    else -> ""
                }
                terminalStream.add("• Bioinformatics Resolved [${optionsList[selectedOpt]}]:")
                terminalStream.add("  - Out: $bioResult")
                historySave("Bioinfo ${optionsList[selectedOpt]}", bioResult)
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1ABC9C)),
            modifier = Modifier.fillMaxWidth().testTag("bio_process_button"),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text("EXECUTE BIOINFORMATICS TASK", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }

        if (bioResult.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .background(Color(0xFF26262F), RoundedCornerShape(4.dp))
                    .padding(6.dp)
            ) {
                Text("Result: $bioResult", color = Color(0xFF9EFFAC), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

// 14. UNIT CONVERTER
@Composable
fun UnitConverterUi(
    terminalStream: MutableList<String>,
    historySave: (String, String) -> Unit
) {
    var ipAmount by remember { mutableStateOf("1.5") }
    // Categories: Mass, Volume, Temperature
    var quantityCategory by remember { mutableStateOf(0) } // 0=Mass, 1=Volume, 2=Temperature
    var fromUnit by remember { mutableStateOf("g") }
    var toUnit by remember { mutableStateOf("mg") }
    var conversionAns by remember { mutableStateOf("") }

    val categories = listOf("Mass", "Volume", "Temperature")
    val unitsList = listOf(
        listOf("kg", "g", "mg", "µg"),
        listOf("L", "mL", "µL"),
        listOf("Celsius", "Kelvin", "Fahrenheit")
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Text("OFFLINE METRIC SCIENTIFIC UNIT TRANSLATOR", color = Color(0xFFE74C3C), fontSize = 11.sp, fontWeight = FontWeight.Bold)

        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            categories.forEachIndexed { idx, item ->
                val chosen = quantityCategory == idx
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (chosen) Color(0xFFE74C3C) else Color(0xFF32323D))
                        .clickable {
                            quantityCategory = idx
                            val units = unitsList[idx]
                            fromUnit = units[0]
                            toUnit = units[1]
                            conversionAns = ""
                        }
                        .padding(vertical = 4.dp), contentAlignment = Alignment.Center
                ) {
                    Text(item, color = if (chosen) Color.Black else Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = ipAmount, onValueChange = { ipAmount = it },
                label = { Text("Value", fontSize = 10.sp) },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                modifier = Modifier.weight(1f).height(46.dp), textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            )
            Spacer(modifier = Modifier.width(4.dp))
            OutlinedTextField(
                value = fromUnit, onValueChange = { fromUnit = it },
                label = { Text("From Unit", fontSize = 10.sp) },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                modifier = Modifier.weight(1f).height(46.dp), textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            )
            Spacer(modifier = Modifier.width(4.dp))
            OutlinedTextField(
                value = toUnit, onValueChange = { toUnit = it },
                label = { Text("To Unit", fontSize = 10.sp) },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                modifier = Modifier.weight(1f).height(46.dp), textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Button(
            onClick = {
                val amnt = ipAmount.toDoubleOrNull()
                if (amnt != null) {
                    val ans = when (quantityCategory) {
                        0 -> UnitConverter.convertMass(amnt, fromUnit, toUnit)
                        1 -> UnitConverter.convertVolume(amnt, fromUnit, toUnit)
                        2 -> UnitConverter.convertTemperature(amnt, fromUnit, toUnit)
                        else -> 0.0
                    }
                    conversionAns = "$amnt $fromUnit = ${String.format("%.4g", ans)} $toUnit"
                    terminalStream.add("• Conversion SUCCESS: $conversionAns")
                    historySave("Unit Conversion: $fromUnit to $toUnit", conversionAns)
                } else {
                    conversionAns = "INSUFFICIENT DATA"
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE74C3C)),
            modifier = Modifier.fillMaxWidth().testTag("convert_button"),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text("CONVERT UNITS", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }

        if (conversionAns.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(conversionAns, color = Color(0xFFFF8E8E), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
    }
}

// 15. FORMULA DATABASE
@Composable
fun FormulaDatabaseUi(onSelectFormula: (FormulaRecord) -> Unit) {
    var textSelectedCategory by remember { mutableStateOf("Gas Laws") }
    val categories = listOf("Gas Laws", "Thermodynamics", "Equilibrium", "Electrochemistry", "Colligative Properties")

    val recordsFiltered = remember(textSelectedCategory) {
        FormulaDatabase.db.filter { it.category == textSelectedCategory }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("CHEMISTRY/BIOPHYSICAL FORMULA BASE", color = Color(0xFF2FA2B1), fontSize = 11.sp, fontWeight = FontWeight.Bold)

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            categories.forEach { cat ->
                val active = textSelectedCategory == cat
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (active) Color(0xFF2FA2B1) else Color(0xFF32323D))
                        .clickable { textSelectedCategory = cat }
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(cat, color = if (active) Color.Black else Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(recordsFiltered) { record ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF26262F))
                        .clickable { onSelectFormula(record) }
                        .padding(6.dp)
                ) {
                    Text(record.name, color = Color(0xFF4FC3F7), fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Text("Equation: ${record.formulaString}", color = Color(0xFF9EFFAC), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Text(record.description, color = Color.LightGray, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

// 16. HISTORY LOGS SHOWCASE
@Composable
fun HistoryUi(
    historyRecords: List<HistoryRecord>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onShareCsv: () -> Unit,
    onClear: () -> Unit
) {
    val filtered = remember(historyRecords.size, searchQuery) {
        historyRecords.filter {
            it.category.contains(searchQuery, ignoreCase = true) ||
                    it.details.contains(searchQuery, ignoreCase = true) ||
                    it.result.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("CALCULATIONS PERSISTENT HISTORY LOGS", color = Color(0xFFE67E22), fontSize = 11.sp, fontWeight = FontWeight.Bold)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedTextField(
                value = searchQuery, onValueChange = onSearchChange,
                placeholder = { Text("Search logs...", color = Color.Gray, fontSize = 10.sp) },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                modifier = Modifier.weight(1f).height(44.dp),
                textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            )

            Button(
                onClick = onShareCsv,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27AE60)),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.height(44.dp).testTag("share_csv_button")
            ) {
                Icon(Icons.Default.Share, contentDescription = "", tint = Color.White, modifier = Modifier.size(14.dp))
            }

            Button(
                onClick = onClear,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC0392B)),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.height(44.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "", tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                Text("No matching search records found offline.", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filtered) { record ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF26262F))
                            .padding(6.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("[${record.timestamp}] ${record.category}", color = Color(0xFFE67E22), fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Text("Inputs: ${record.details}", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Text("Result: ${record.result}", color = Color(0xFF9EFFAC), fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

// 17. GRAPH PLOTTER
@Composable
fun GraphPlotterUi() {
    val plots = listOf("Kinetics Decay", "Growth Logistic", "Boyle Isotherm")
    var selectedPlotIndex by remember { mutableStateOf(0) }
    var graphCoefficient by remember { mutableStateOf(0.4f) } // Slider coefficient for kinetics k, growth r

    Column(modifier = Modifier.fillMaxSize()) {
        Text("BIOPHYSICAL KINETIC GRAPHING (Compose Canvas)", color = Color(0xFF3498DB), fontSize = 10.sp, fontWeight = FontWeight.Bold)

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            plots.forEachIndexed { idx, label ->
                val active = selectedPlotIndex == idx
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (active) Color(0xFF3498DB) else Color(0xFF32323D))
                        .clickable { selectedPlotIndex = idx }
                        .padding(vertical = 4.dp), contentAlignment = Alignment.Center
                ) {
                    Text(label, color = if (active) Color.Black else Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        // Coefficient slider representing variables changing kinetics/isotherms
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Coefficient (k/r): ${String.format("%.2f", graphCoefficient)}", color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(110.dp))
            Slider(
                value = graphCoefficient,
                onValueChange = { graphCoefficient = it },
                valueRange = 0.05f..1.5f,
                modifier = Modifier.weight(1f).height(18.dp)
            )
        }

        // Custom Canvas drawing the mathematical curves
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF0C0F12))
                .border(2.dp, Color(0xFF2C3E50), RoundedCornerShape(6.dp))
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                val width = size.width
                val height = size.height

                // Draw background coordinates grid axes
                val gridLines = 8
                for (i in 1 until gridLines) {
                    val x = width * i / gridLines
                    val y = height * i / gridLines
                    drawLine(Color(0x1A76C786), Offset(x, 0f), Offset(x, height), strokeWidth = 1f)
                    drawLine(Color(0x1A76C786), Offset(0f, y), Offset(width, y), strokeWidth = 1f)
                }

                // Draw axis
                drawLine(Color.Gray, Offset(0f, height), Offset(width, height), strokeWidth = 3f)
                drawLine(Color.Gray, Offset(0f, 0f), Offset(0f, height), strokeWidth = 3f)

                // Plot curves
                val path = Path()
                path.moveTo(0f, height)

                val pointsCount = 100
                for (i in 0..pointsCount) {
                    val t = i.toFloat() / pointsCount
                    val xPos = t * width
                    
                    val yVal = when (selectedPlotIndex) {
                        0 -> {
                            // Kinetics Decay: A(t) = A0 * e^(-k * t)
                            val k = graphCoefficient
                            exp(-k * (t * 5f)) // Simulated over t=[0, 5]
                        }
                        1 -> {
                            // Growth Logistic curve: N(t) = 1 / (1 + e^-(r* (t - 0.5)))
                            val r = graphCoefficient * 10
                            1f / (1f + exp(-r * (t - 0.5f)))
                        }
                        2 -> {
                            // Boyle Isotherm: P = inverse(V)
                            val scale = graphCoefficient
                            val v = t * 4f + 0.5f
                            (scale / v) * 0.7f
                        }
                        else -> 0f
                    }

                    // Translate mathematical y (0.0 to 1.0) to screen pixel coordinates (height to 0)
                    val clampedY = yVal.coerceIn(0f, 1f)
                    val yPos = height - (clampedY * height * 0.9f)

                    if (i == 0) {
                        path.moveTo(xPos, yPos)
                    } else {
                        path.lineTo(xPos, yPos)
                    }
                }

                drawPath(
                    path = path,
                    color = Color(0xFF00FFCC),
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }
    }
}
