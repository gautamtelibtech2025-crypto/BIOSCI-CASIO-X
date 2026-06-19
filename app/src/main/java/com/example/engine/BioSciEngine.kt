package com.example.engine

import kotlin.math.*

// ==========================================
// 1. RECURSIVE DESCENT MATH PARSER
// ==========================================
class MathParser(private val str: String) {
    private var pos = -1
    private var ch = 0

    private fun nextChar() {
        pos++
        ch = if (pos < str.length) str[pos].code else -1
    }

    private fun eat(charToEat: Int): Boolean {
        while (ch == ' '.code) nextChar()
        if (ch == charToEat) {
            nextChar()
            return true
        }
        return false
    }

    fun parse(): Double {
        nextChar()
        val x = parseExpression()
        if (pos < str.length) throw RuntimeException("Unexpected character: " + ch.toChar())
        return x
    }

    private fun parseExpression(): Double {
        var x = parseTerm()
        while (true) {
            when {
                eat('+'.code) -> x += parseTerm()
                eat('-'.code) -> x -= parseTerm()
                else -> return x
            }
        }
    }

    private fun parseTerm(): Double {
        var x = parseFactor()
        while (true) {
            when {
                eat('*'.code) -> x *= parseFactor()
                eat('/'.code) -> x /= parseFactor()
                eat('%'.code) -> x %= parseFactor()
                else -> return x
            }
        }
    }

    private fun parseFactor(): Double {
        if (eat('+'.code)) return parseFactor()
        if (eat('-'.code)) return -parseFactor()

        var x: Double
        val startPos = this.pos
        if (eat('('.code)) {
            x = parseExpression()
            eat(')'.code)
        } else if ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) {
            while ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code || ch == 'E'.code || ch == 'e'.code) {
                if (ch == 'E'.code || ch == 'e'.code) {
                    nextChar()
                    if (ch == '-'.code || ch == '+'.code) nextChar()
                } else {
                    nextChar()
                }
            }
            val numStr = str.substring(startPos, this.pos)
            x = numStr.toDoubleOrNull() ?: 0.0
        } else if ((ch >= 'a'.code && ch <= 'z'.code) || (ch >= 'A'.code && ch <= 'Z'.code)) {
            while ((ch >= 'a'.code && ch <= 'z'.code) || (ch >= 'A'.code && ch <= 'Z'.code) || (ch >= '0'.code && ch <= '9'.code)) {
                nextChar()
            }
            val name = str.substring(startPos, this.pos).lowercase()
            if (name == "pi") return Math.PI
            if (name == "e") return Math.E

            // read nested argument
            x = parseFactor()
            x = when (name) {
                "sqrt" -> sqrt(x)
                "cbrt" -> cbrt(x)
                "sin" -> sin(x)
                "cos" -> cos(x)
                "tan" -> tan(x)
                "asin" -> asin(x)
                "acos" -> acos(x)
                "atan" -> atan(x)
                "sinh" -> sinh(x)
                "cosh" -> cosh(x)
                "tanh" -> tanh(x)
                "log", "log10" -> log10(x)
                "ln" -> ln(x)
                "exp" -> exp(x)
                else -> throw RuntimeException("Unknown function: $name")
            }
        } else {
            throw RuntimeException("Unexpected character: " + ch.toChar())
        }

        if (eat('^'.code)) {
            x = x.pow(parseFactor())
        }
        if (eat('!'.code)) {
            x = factorial(x.roundToInt()).toDouble()
        }
        return x
    }

    private fun factorial(n: Int): Long {
        if (n < 0) return 0
        var f: Long = 1
        for (i in 1..n) {
            f *= i
        }
        return f
    }
}

// ==========================================
// 2. PERIODIC TABLE & CHEMICAL ANALYSIS
// ==========================================
data class Element(
    val number: Int,
    val symbol: String,
    val name: String,
    val mass: Double,
    val group: Int,
    val period: Int,
    val electroNegativity: Double?,
    val stateAtStp: String
)

object PeriodicTable {
    val elementsList = listOf(
        Element(1, "H", "Hydrogen", 1.008, 1, 1, 2.20, "Gas"),
        Element(2, "He", "Helium", 4.0026, 18, 1, null, "Gas"),
        Element(3, "Li", "Lithium", 6.94, 1, 2, 0.98, "Solid"),
        Element(4, "Be", "Beryllium", 9.0122, 2, 2, 1.57, "Solid"),
        Element(5, "B", "Boron", 10.81, 13, 2, 2.04, "Solid"),
        Element(6, "C", "Carbon", 12.011, 14, 2, 2.55, "Solid"),
        Element(7, "N", "Nitrogen", 14.007, 15, 2, 3.04, "Gas"),
        Element(8, "O", "Oxygen", 15.999, 16, 2, 3.44, "Gas"),
        Element(9, "F", "Fluorine", 18.998, 17, 2, 3.98, "Gas"),
        Element(10, "Ne", "Neon", 20.180, 18, 2, null, "Gas"),
        Element(11, "Na", "Sodium", 22.990, 1, 3, 0.93, "Solid"),
        Element(12, "Mg", "Magnesium", 24.305, 2, 3, 1.31, "Solid"),
        Element(13, "Al", "Aluminium", 26.982, 13, 3, 1.61, "Solid"),
        Element(14, "Si", "Silicon", 28.085, 14, 3, 1.90, "Solid"),
        Element(15, "P", "Phosphorus", 30.974, 15, 3, 2.19, "Solid"),
        Element(16, "S", "Sulfur", 32.06, 16, 3, 2.58, "Solid"),
        Element(17, "Cl", "Chlorine", 35.45, 17, 3, 3.16, "Gas"),
        Element(18, "Ar", "Argon", 39.948, 18, 3, null, "Gas"),
        Element(19, "K", "Potassium", 39.098, 1, 4, 0.82, "Solid"),
        Element(20, "Ca", "Calcium", 40.078, 2, 4, 1.00, "Solid"),
        Element(21, "Sc", "Scandium", 44.956, 3, 4, 1.36, "Solid"),
        Element(22, "Ti", "Titanium", 47.867, 4, 4, 1.54, "Solid"),
        Element(23, "V", "Vanadium", 50.942, 5, 4, 1.63, "Solid"),
        Element(24, "Cr", "Chromium", 51.996, 6, 4, 1.66, "Solid"),
        Element(25, "Mn", "Manganese", 54.938, 7, 4, 1.55, "Solid"),
        Element(26, "Fe", "Iron", 55.845, 8, 4, 1.83, "Solid"),
        Element(27, "Co", "Cobalt", 58.933, 9, 4, 1.88, "Solid"),
        Element(28, "Ni", "Nickel", 58.693, 10, 4, 1.91, "Solid"),
        Element(29, "Cu", "Copper", 63.546, 11, 4, 1.90, "Solid"),
        Element(30, "Zn", "Zinc", 65.38, 12, 4, 1.65, "Solid"),
        Element(31, "Ga", "Gallium", 69.723, 13, 4, 1.81, "Solid"),
        Element(32, "Ge", "Germanium", 72.63, 14, 4, 2.01, "Solid"),
        Element(33, "As", "Arsenic", 74.922, 15, 4, 2.18, "Solid"),
        Element(34, "Se", "Selenium", 78.971, 16, 4, 2.55, "Solid"),
        Element(35, "Br", "Bromine", 79.904, 17, 4, 2.96, "Liquid"),
        Element(36, "Kr", "Krypton", 83.798, 18, 4, 3.00, "Gas"),
        Element(37, "Rb", "Rubidium", 85.468, 1, 5, 0.82, "Solid"),
        Element(38, "Sr", "Strontium", 87.62, 2, 5, 0.95, "Solid"),
        Element(39, "Y", "Yttrium", 88.906, 3, 5, 1.22, "Solid"),
        Element(40, "Zr", "Zirconium", 91.224, 4, 5, 1.33, "Solid"),
        Element(41, "Nb", "Niobium", 92.906, 5, 5, 1.60, "Solid"),
        Element(42, "Mo", "Molybdenum", 95.95, 6, 5, 2.16, "Solid"),
        Element(43, "Tc", "Technetium", 98.0, 7, 5, 1.9, "Solid"),
        Element(44, "Ru", "Ruthenium", 101.07, 8, 5, 2.2, "Solid"),
        Element(45, "Rh", "Rhodium", 102.91, 9, 5, 2.28, "Solid"),
        Element(46, "Pd", "Palladium", 106.42, 10, 5, 2.20, "Solid"),
        Element(47, "Ag", "Silver", 107.87, 11, 5, 1.93, "Solid"),
        Element(48, "Cd", "Cadmium", 112.41, 12, 5, 1.69, "Solid"),
        Element(49, "In", "Indium", 114.82, 13, 5, 1.78, "Solid"),
        Element(50, "Sn", "Tin", 118.71, 14, 5, 1.96, "Solid"),
        Element(51, "Sb", "Antimony", 121.76, 15, 5, 2.05, "Solid"),
        Element(52, "Te", "Tellurium", 127.60, 16, 5, 2.10, "Solid"),
        Element(53, "I", "Iodine", 126.90, 17, 5, 2.66, "Solid"),
        Element(54, "Xe", "Xenon", 131.29, 18, 5, 2.60, "Gas"),
        Element(55, "Cs", "Caesium", 132.91, 1, 6, 0.79, "Solid"),
        Element(56, "Ba", "Barium", 137.33, 2, 6, 0.89, "Solid"),
        Element(57, "La", "Lanthanum", 138.91, 3, 6, 1.10, "Solid"),
        Element(74, "W", "Tungsten", 183.84, 6, 6, 2.36, "Solid"),
        Element(78, "Pt", "Platinum", 195.08, 10, 6, 2.28, "Solid"),
        Element(79, "Au", "Gold", 196.97, 11, 6, 2.54, "Solid"),
        Element(80, "Hg", "Mercury", 200.59, 12, 6, 2.00, "Liquid"),
        Element(82, "Pb", "Lead", 207.2, 14, 6, 2.33, "Solid"),
        Element(92, "U", "Uranium", 238.03, 3, 7, 1.38, "Solid")
    )

    private val symbolMap = elementsList.associateBy { it.symbol }

    fun findSymbol(sym: String): Element? = symbolMap[sym]
    fun findNumber(num: Int): Element? = elementsList.find { it.number == num }
    fun findName(name: String): Element? = elementsList.find { it.name.lowercase() == name.lowercase() }
}

// Recursive chemical parser helper
fun parseFormula(formula: String): Map<String, Double> {
    val result = mutableMapOf<String, Double>()
    var i = 0
    val n = formula.length

    fun parseGroup(): Map<String, Double> {
        val group = mutableMapOf<String, Double>()
        while (i < n) {
            val c = formula[i]
            if (c == ')') {
                break
            } else if (c == '(') {
                i++ // eat '('
                val subGroup = parseGroup()
                if (i < n && formula[i] == ')') {
                    i++ // eat ')'
                    var countStr = ""
                    while (i < n && formula[i].isDigit()) {
                        countStr += formula[i]
                        i++
                    }
                    val multi = if (countStr.isNotEmpty()) countStr.toDouble() else 1.0
                    for ((elem, count) in subGroup) {
                        group[elem] = (group[elem] ?: 0.0) + count * multi
                    }
                }
            } else if (c.isLetter() && c.isUpperCase()) {
                var elem = c.toString()
                i++
                while (i < n && formula[i].isLetter() && formula[i].isLowerCase()) {
                    elem += formula[i]
                    i++
                }
                var countStr = ""
                while (i < n && formula[i].isDigit()) {
                    countStr += formula[i]
                    i++
                }
                val count = if (countStr.isNotEmpty()) countStr.toDouble() else 1.0
                group[elem] = (group[elem] ?: 0.0) + count
            } else {
                i++
            }
        }
        return group
    }

    try {
        val map = parseGroup()
        for ((elem, count) in map) {
            result[elem] = (result[elem] ?: 0.0) + count
        }
    } catch (e: Exception) {
        // Return empty or partial map in case of parse error
    }
    return result
}

fun calculateMolecularMass(formula: String): Double {
    val elementMap = parseFormula(formula)
    if (elementMap.isEmpty()) return 0.0
    var totalMass = 0.0
    for ((elem, count) in elementMap) {
        val atomicWeight = PeriodicTable.findSymbol(elem)?.mass ?: 0.0
        if (atomicWeight == 0.0) return 0.0 // unknown element makes the whole weight uncomputable
        totalMass += atomicWeight * count
    }
    return totalMass
}

// ==========================================
// 3. CHEMISTRY GENERIC SOLVER ENGINE
// ==========================================
enum class ChemVar(val displayName: String, val unit: String) {
    MASS("Mass of solute", "g"),
    MOLES("Moles of solute", "mol"),
    VOLUME("Volume of solution", "L"),
    MOL_WEIGHT("Molecular Weight", "g/mol"),
    MOLARITY("Molarity", "M (mol/L)"),
    NORMALITY("Normality", "N"),
    N_FACTOR("n-factor", ""),
    STRENGTH("Strength", "g/L"),
    EQUIVALENT_WEIGHT("Equivalent Weight", "g/eq"),
    SOLUTE_MASS("Mass of solute (ppm/ppb)", "g"),
    SOLUTION_MASS("Mass of solution (ppm/ppb)", "g"),
    PPM("ppm Concentration", "ppm"),
    PPB("ppb Concentration", "ppb"),
    PH("pH Level", ""),
    POH("pOH Level", ""),
    H_CONC("H+ concentration", "M"),
    OH_CONC("OH- concentration", "M"),
    DILUTION_V1("V1 (initial Vol)", "mL"),
    DILUTION_C1("C1 (initial Conc)", "M"),
    DILUTION_V2("V2 (final Vol)", "mL"),
    DILUTION_C2("C2 (final Conc)", "M")
}

class ChemistrySolverEngine {
    fun solve(target: ChemVar, inputs: Map<ChemVar, Double>, stepsLog: MutableList<String>): Double? {
        val knowns = inputs.toMutableMap()
        
        // Propagate formulas in loop
        var updated = true
        var iteration = 0
        while (updated && iteration < 10) {
            updated = false
            
            // Helper to secure a calculation
            fun derive(v: ChemVar, value: Double, reason: String) {
                if (!knowns.containsKey(v)) {
                    knowns[v] = value
                    stepsLog.add("• Calculated ${v.displayName} = ${String.format("%.4f", value)} ${v.unit} [$reason]")
                    updated = true
                }
            }

            // 1. mass / mol_weight <-> moles
            if (knowns.containsKey(ChemVar.MASS) && knowns.containsKey(ChemVar.MOL_WEIGHT)) {
                val mass = knowns[ChemVar.MASS]!!
                val mw = knowns[ChemVar.MOL_WEIGHT]!!
                if (mw > 0.0) derive(ChemVar.MOLES, mass / mw, "Moles = Mass / MolecularWeight")
            }
            if (knowns.containsKey(ChemVar.MOLES) && knowns.containsKey(ChemVar.MOL_WEIGHT)) {
                derive(ChemVar.MASS, knowns[ChemVar.MOLES]!! * knowns[ChemVar.MOL_WEIGHT]!!, "Mass = Moles * MolecularWeight")
            }
            if (knowns.containsKey(ChemVar.MASS) && knowns.containsKey(ChemVar.MOLES)) {
                val moles = knowns[ChemVar.MOLES]!!
                if (moles > 0.0) derive(ChemVar.MOL_WEIGHT, knowns[ChemVar.MASS]!! / moles, "MolecularWeight = Mass / Moles")
            }

            // 2. moles / volume <-> molarity
            if (knowns.containsKey(ChemVar.MOLES) && knowns.containsKey(ChemVar.VOLUME)) {
                val vol = knowns[ChemVar.VOLUME]!!
                if (vol > 0.0) derive(ChemVar.MOLARITY, knowns[ChemVar.MOLES]!! / vol, "Molarity = Moles / Volume")
            }
            if (knowns.containsKey(ChemVar.MOLARITY) && knowns.containsKey(ChemVar.VOLUME)) {
                derive(ChemVar.MOLES, knowns[ChemVar.MOLARITY]!! * knowns[ChemVar.VOLUME]!!, "Moles = Molarity * Volume")
            }
            if (knowns.containsKey(ChemVar.MOLES) && knowns.containsKey(ChemVar.MOLARITY)) {
                val molarity = knowns[ChemVar.MOLARITY]!!
                if (molarity > 0.0) derive(ChemVar.VOLUME, knowns[ChemVar.MOLES]!! / molarity, "Volume = Moles / Molarity")
            }

            // 3. molarity * n_factor <-> normality
            if (knowns.containsKey(ChemVar.MOLARITY) && knowns.containsKey(ChemVar.N_FACTOR)) {
                derive(ChemVar.NORMALITY, knowns[ChemVar.MOLARITY]!! * knowns[ChemVar.N_FACTOR]!!, "Normality = Molarity * n-factor")
            }
            if (knowns.containsKey(ChemVar.NORMALITY) && knowns.containsKey(ChemVar.N_FACTOR)) {
                val f = knowns[ChemVar.N_FACTOR]!!
                if (f > 0.0) derive(ChemVar.MOLARITY, knowns[ChemVar.NORMALITY]!! / f, "Molarity = Normality / n-factor")
            }
            if (knowns.containsKey(ChemVar.NORMALITY) && knowns.containsKey(ChemVar.MOLARITY)) {
                val m = knowns[ChemVar.MOLARITY]!!
                if (m > 0.0) derive(ChemVar.N_FACTOR, knowns[ChemVar.NORMALITY]!! / m, "n-factor = Normality / Molarity")
            }

            // 4. mol_weight / n_factor <-> equivalent_weight
            if (knowns.containsKey(ChemVar.MOL_WEIGHT) && knowns.containsKey(ChemVar.N_FACTOR)) {
                val f = knowns[ChemVar.N_FACTOR]!!
                if (f > 0.0) derive(ChemVar.EQUIVALENT_WEIGHT, knowns[ChemVar.MOL_WEIGHT]!! / f, "EquivalentWeight = MolecularWeight / n-factor")
            }
            if (knowns.containsKey(ChemVar.EQUIVALENT_WEIGHT) && knowns.containsKey(ChemVar.N_FACTOR)) {
                derive(ChemVar.MOL_WEIGHT, knowns[ChemVar.EQUIVALENT_WEIGHT]!! * knowns[ChemVar.N_FACTOR]!!, "MolecularWeight = EquivalentWeight * n-factor")
            }

            // 5. mass / volume <-> strength
            if (knowns.containsKey(ChemVar.MASS) && knowns.containsKey(ChemVar.VOLUME)) {
                val vol = knowns[ChemVar.VOLUME]!!
                if (vol > 0.0) derive(ChemVar.STRENGTH, knowns[ChemVar.MASS]!! / vol, "Strength = Mass / Volume")
            }
            if (knowns.containsKey(ChemVar.STRENGTH) && knowns.containsKey(ChemVar.VOLUME)) {
                derive(ChemVar.MASS, knowns[ChemVar.STRENGTH]!! * knowns[ChemVar.VOLUME]!!, "Mass = Strength * Volume")
            }
            if (knowns.containsKey(ChemVar.MASS) && knowns.containsKey(ChemVar.STRENGTH)) {
                val strength = knowns[ChemVar.STRENGTH]!!
                if (strength > 0.0) derive(ChemVar.VOLUME, knowns[ChemVar.MASS]!! / strength, "Volume = Mass / Strength")
            }

            // 6. molarity * mol_weight <-> strength
            if (knowns.containsKey(ChemVar.MOLARITY) && knowns.containsKey(ChemVar.MOL_WEIGHT)) {
                derive(ChemVar.STRENGTH, knowns[ChemVar.MOLARITY]!! * knowns[ChemVar.MOL_WEIGHT]!!, "Strength = Molarity * MolecularWeight")
            }
            if (knowns.containsKey(ChemVar.STRENGTH) && knowns.containsKey(ChemVar.MOL_WEIGHT)) {
                val mw = knowns[ChemVar.MOL_WEIGHT]!!
                if (mw > 0.0) derive(ChemVar.MOLARITY, knowns[ChemVar.STRENGTH]!! / mw, "Molarity = Strength / MolecularWeight")
            }

            // 7. ph <-> poh <-> pH Conc
            if (knowns.containsKey(ChemVar.PH)) {
                val ph = knowns[ChemVar.PH]!!
                derive(ChemVar.POH, 14.0 - ph, "pOH = 14 - pH")
                derive(ChemVar.H_CONC, 10.0.pow(-ph), "H+ = 10^-pH")
            }
            if (knowns.containsKey(ChemVar.POH)) {
                val poh = knowns[ChemVar.POH]!!
                derive(ChemVar.PH, 14.0 - poh, "pH = 14 - pOH")
                derive(ChemVar.OH_CONC, 10.0.pow(-poh), "OH- = 10^-pOH")
            }
            if (knowns.containsKey(ChemVar.H_CONC)) {
                val h = knowns[ChemVar.H_CONC]!!
                if (h > 0.0) derive(ChemVar.PH, -log10(h), "pH = -log10[H+]")
            }
            if (knowns.containsKey(ChemVar.OH_CONC)) {
                val oh = knowns[ChemVar.OH_CONC]!!
                if (oh > 0.0) derive(ChemVar.POH, -log10(oh), "pOH = -log10[OH-]")
            }

            // 8. ppm/ppb
            if (knowns.containsKey(ChemVar.SOLUTE_MASS) && knowns.containsKey(ChemVar.SOLUTION_MASS)) {
                val sm = knowns[ChemVar.SOLUTE_MASS]!!
                val soln = knowns[ChemVar.SOLUTION_MASS]!!
                if (soln > 0.0) {
                    derive(ChemVar.PPM, (sm / soln) * 1e6, "ppm = (Solute / Solution) * 10^6")
                    derive(ChemVar.PPB, (sm / soln) * 1e9, "ppb = (Solute / Solution) * 10^9")
                }
            }
            if (knowns.containsKey(ChemVar.PPM) && knowns.containsKey(ChemVar.SOLUTION_MASS)) {
                derive(ChemVar.SOLUTE_MASS, knowns[ChemVar.SOLUTION_MASS]!! * knowns[ChemVar.PPM]!! * 1e-6, "SoluteMass = SolutionMass * ppm * 10^-6")
            }
            if (knowns.containsKey(ChemVar.PPB) && knowns.containsKey(ChemVar.SOLUTION_MASS)) {
                derive(ChemVar.SOLUTE_MASS, knowns[ChemVar.SOLUTION_MASS]!! * knowns[ChemVar.PPB]!! * 1e-9, "SoluteMass = SolutionMass * ppb * 10^-9")
            }

            // 9. Dilution V1 C1 = V2 C2
            val dilKnown = listOf(ChemVar.DILUTION_V1, ChemVar.DILUTION_C1, ChemVar.DILUTION_V2, ChemVar.DILUTION_C2).filter { knowns.containsKey(it) }
            if (dilKnown.size == 3) {
                if (!knowns.containsKey(ChemVar.DILUTION_V1)) {
                    val c1 = knowns[ChemVar.DILUTION_C1]!!
                    if (c1 > 0.0) derive(ChemVar.DILUTION_V1, (knowns[ChemVar.DILUTION_C2]!! * knowns[ChemVar.DILUTION_V2]!!) / c1, "V1 = V2 * C2 / C1")
                }
                if (!knowns.containsKey(ChemVar.DILUTION_C1)) {
                    val v1 = knowns[ChemVar.DILUTION_V1]!!
                    if (v1 > 0.0) derive(ChemVar.DILUTION_C1, (knowns[ChemVar.DILUTION_C2]!! * knowns[ChemVar.DILUTION_V2]!!) / v1, "C1 = V2 * C2 / V1")
                }
                if (!knowns.containsKey(ChemVar.DILUTION_V2)) {
                    val c2 = knowns[ChemVar.DILUTION_C2]!!
                    if (c2 > 0.0) derive(ChemVar.DILUTION_V2, (knowns[ChemVar.DILUTION_C1]!! * knowns[ChemVar.DILUTION_V1]!!) / c2, "V2 = V1 * C1 / C2")
                }
                if (!knowns.containsKey(ChemVar.DILUTION_C2)) {
                    val v2 = knowns[ChemVar.DILUTION_V2]!!
                    if (v2 > 0.0) derive(ChemVar.DILUTION_C2, (knowns[ChemVar.DILUTION_C1]!! * knowns[ChemVar.DILUTION_V1]!!) / v2, "C2 = V1 * C1 / V2")
                }
            }

            iteration++
        }
        
        return knowns[target]
    }
}

// ==========================================
// 4. BIOINFORMATICS SOLVERS HELPERS
// ==========================================
object BioinformaticsEngine {
    private val codonTable = mapOf(
        "UUU" to "F", "UUC" to "F",
        "UUA" to "L", "UUG" to "L", "CUU" to "L", "CUC" to "L", "CUA" to "L", "CUG" to "L",
        "AUU" to "I", "AUC" to "I", "AUA" to "I",
        "AUG" to "M",
        "GUU" to "V", "GUC" to "V", "GUA" to "V", "GUG" to "V",
        "UCU" to "S", "UCC" to "S", "UCA" to "S", "UCG" to "S", "AGU" to "S", "AGC" to "S",
        "CCU" to "P", "CCC" to "P", "CCA" to "P", "CCG" to "P",
        "ACU" to "T", "ACC" to "T", "ACA" to "T", "ACG" to "T",
        "GCU" to "A", "GCC" to "A", "GCA" to "A", "GCG" to "A",
        "UAU" to "Y", "UAC" to "Y",
        "UAA" to "*", "UAG" to "*", "UGA" to "*",
        "CAU" to "H", "CAC" to "H",
        "CAA" to "Q", "CAG" to "Q",
        "AAU" to "N", "AAC" to "N",
        "AAA" to "K", "AAG" to "K",
        "GAU" to "D", "GAC" to "D",
        "GAA" to "E", "GAG" to "E",
        "UGU" to "C", "UGC" to "C",
        "UGG" to "W",
        "CGU" to "R", "CGC" to "R", "CGA" to "R", "CGG" to "R", "AGA" to "R", "AGG" to "R",
        "GGU" to "G", "GGC" to "G", "GGA" to "G", "GGG" to "G"
    )

    private val aaWeights = mapOf(
        'A' to 89.09, 'R' to 174.20, 'N' to 132.12, 'D' to 133.10, 'C' to 121.16,
        'E' to 147.13, 'Q' to 146.15, 'G' to 75.07, 'H' to 155.16, 'I' to 131.17,
        'L' to 131.17, 'K' to 146.19, 'M' to 149.21, 'F' to 165.19, 'P' to 115.13,
        'S' to 105.09, 'T' to 119.12, 'W' to 204.23, 'Y' to 181.19, 'V' to 117.15
    )

    fun calculateGcContent(dna: String): Double {
        val clean = dna.uppercase().filter { it in "ATGCU" }
        if (clean.isEmpty()) return 0.0
        val gc = clean.count { it == 'G' || it == 'C' }
        return (gc.toDouble() / clean.length) * 100.0
    }

    fun transcribeDnaToRna(dna: String): String {
        return dna.uppercase().replace('T', 'U')
    }

    fun reverseComplement(seq: String): String {
        return seq.uppercase().reversed().map {
            when (it) {
                'A' -> 'T'
                'T' -> 'A'
                'U' -> 'A'
                'G' -> 'C'
                'C' -> 'G'
                else -> it
            }
        }.joinToString("")
    }

    fun translateRnaToProtein(rna: String): String {
        val clean = rna.uppercase().replace('T', 'U').filter { it in "AUGC" }
        val protein = StringBuilder()
        for (i in 0 until (clean.length - 2) step 3) {
            val codon = clean.substring(i, i + 3)
            val aa = codonTable[codon] ?: "?"
            protein.append(aa)
        }
        return protein.toString()
    }

    fun calculateProteinMw(protein: String): Double {
        val clean = protein.uppercase().filter { it in aaWeights.keys }
        if (clean.isEmpty()) return 0.0
        var total = 0.0
        for (aa in clean) {
            total += aaWeights[aa] ?: 0.0
        }
        // Subtract water weight (18.02 g/mol) for peptide bonds linking residues
        val peptideBonds = clean.length - 1
        if (peptideBonds > 0) {
            total -= peptideBonds * 18.015
        }
        return total
    }

    fun getAminoAcidFrequency(protein: String): Map<Char, Double> {
        val clean = protein.uppercase().filter { it.isLetter() }
        if (clean.isEmpty()) return emptyMap()
        val total = clean.length.toDouble()
        return clean.groupingBy { it }.eachCount().mapValues { (it.value / total) * 100.0 }
    }

    fun getCodonDistribution(dna: String): Map<String, Int> {
        val clean = dna.uppercase().replace('T', 'U').filter { it in "AUGC" }
        val counts = mutableMapOf<String, Int>()
        for (i in 0 until (clean.length - 2) step 3) {
            val codon = clean.substring(i, i + 3)
            counts[codon] = (counts[codon] ?: 0) + 1
        }
        return counts
    }
}

// ==========================================
// 5. OFFLINE UNIT CONVERSION COEFFICIENTS
// ==========================================
object UnitConverter {
    // Converts value in `fromUnit` to target base and then to `toUnit`
    fun convertMass(value: Double, from: String, to: String): Double {
        val toGrams = when (from) {
            "kg" -> 1000.0
            "g" -> 1.0
            "mg" -> 0.001
            "µg" -> 0.000001
            else -> 1.0
        }
        val fromGrams = when (to) {
            "kg" -> 0.001
            "g" -> 1.0
            "mg" -> 1000.0
            "µg" -> 1000000.0
            else -> 1.0
        }
        return value * toGrams * fromGrams
    }

    fun convertVolume(value: Double, from: String, to: String): Double {
        val toLiters = when (from) {
            "L" -> 1.0
            "mL" -> 0.001
            "µL" -> 0.000001
            else -> 1.0
        }
        val fromLiters = when (to) {
            "L" -> 1.0
            "mL" -> 1000.0
            "µL" -> 1000000.0
            else -> 1.0
        }
        return value * toLiters * fromLiters
    }

    fun convertTemperature(value: Double, from: String, to: String): Double {
        val inKelvin = when (from) {
            "Celsius" -> value + 273.15
            "Kelvin" -> value
            "Fahrenheit" -> (value - 32.0) * 5.0 / 9.0 + 273.15
            else -> value
        }
        return when (to) {
            "Celsius" -> inKelvin - 273.15
            "Kelvin" -> inKelvin
            "Fahrenheit" -> (inKelvin - 273.15) * 9.0 / 5.0 + 32.0
            else -> inKelvin
        }
    }

    fun convertMoles(value: Double, from: String, to: String): Double {
        val toMol = when (from) {
            "mol" -> 1.0
            "mmol" -> 0.001
            "µmol" -> 0.000001
            else -> 1.0
        }
        val fromMol = when (to) {
            "mol" -> 1.0
            "mmol" -> 1000.0
            "µmol" -> 1000000.0
            else -> 1.0
        }
        return value * toMol * fromMol
    }

    fun convertConcentration(value: Double, from: String, to: String): Double {
        val toM = when (from) {
            "M" -> 1.0
            "mM" -> 0.001
            "µM" -> 0.000001
            else -> 1.0
        }
        val fromM = when (to) {
            "M" -> 1.0
            "mM" -> 1000.0
            "µM" -> 1000000.0
            else -> 1.0
        }
        return value * toM * fromM
    }
}

// ==========================================
// 6. FORMULA DATABASE MODELS
// ==========================================
data class FormulaRecord(
    val category: String,
    val name: String,
    val formulaString: String,
    val variables: Map<String, String>, // var name to physical description
    val description: String
)

object FormulaDatabase {
    val db = listOf(
        // Gas Laws
        FormulaRecord(
            "Gas Laws",
            "Ideal Gas Law",
            "P * V = n * R * T",
            mapOf("P" to "Pressure (atm)", "V" to "Volume (L)", "n" to "Moles (mol)", "T" to "Temperature (K)", "R" to "Gas Constant = 0.08206 L*atm/(mol*K)"),
            "Calculates state conditions of an ideal gas. Equates pressure, volume, moles and temperature."
        ),
        FormulaRecord(
            "Gas Laws",
            "Boyle's Law",
            "P1 * V1 = P2 * V2",
            mapOf("P1" to "Initial Pressure (atm)", "V1" to "Initial Volume (L)", "P2" to "Final Pressure (atm)", "V2" to "Final Volume (L)"),
            "At constant temperature, the volume of a gas is inversely proportional to its pressure."
        ),
        FormulaRecord(
            "Gas Laws",
            "Charles's Law",
            "V1 / T1 = V2 / T2",
            mapOf("V1" to "Initial Volume (L)", "T1" to "Initial Temp (K)", "V2" to "Final Volume (L)", "T2" to "Final Temp (K)"),
            "At constant pressure, gas volume is directly proportional to its absolute temperature."
        ),
        FormulaRecord(
            "Gas Laws",
            "Gay-Lussac's Law",
            "P1 / T1 = P2 / T2",
            mapOf("P1" to "Initial Pressure (atm)", "T1" to "Initial Temp (K)", "P2" to "Final Pressure (atm)", "T2" to "Final Temp (K)"),
            "At constant volume, gas pressure is directly proportional to its absolute temperature."
        ),
        FormulaRecord(
            "Gas Laws",
            "RMS Gas Velocity",
            "v_rms = sqrt(3 * R * T / M)",
            mapOf("v_rms" to "RMS velocity (m/s)", "T" to "Temperature (K)", "M" to "Molar Mass in kg/mol", "R" to "Gas constant = 8.314 J/(mol*K)"),
            "Calculates the root-mean-square speed of gas particles as a function of temperature and molar mass."
        ),
        FormulaRecord(
            "Gas Laws",
            "Graham's Law",
            "r1 / r2 = sqrt(M2 / M1)",
            mapOf("r1" to "Effusion rate 1", "r2" to "Effusion rate 2", "M1" to "Molar mass gas 1 (g/mol)", "M2" to "Molar mass gas 2 (g/mol)"),
            "The rate of effusion or diffusion of a gas is inversely proportional to the square root of its molar mass."
        ),
        
        // Thermodynamics
        FormulaRecord(
            "Thermodynamics",
            "Gibbs Free Energy Equation",
            "dG = dH - T * dS",
            mapOf("dG" to "Change in Gibbs Energy (kJ/mol)", "dH" to "Change in Enthalpy (kJ/mol)", "T" to "Temperature (K)", "dS" to "Change in Entropy (kJ/(mol*K))"),
            "Determines spontaneous tendency of a reaction. Spontaneous if dG < 0, non-spontaneous if dG > 0."
        ),
        FormulaRecord(
            "Thermodynamics",
            "Specific Heat Capacity",
            "q = m * c * dT",
            mapOf("q" to "Heat Energy Added (J)", "m" to "Mass of Substance (g)", "c" to "Specific Heat Capacity (J/g*C)", "dT" to "Change in Temp (K or C)"),
            "Calculates thermal heat transferred to stable matters."
        ),
        
        // Equilibrium
        FormulaRecord(
            "Equilibrium",
            "Gas/Solvent Equilibrium Relation",
            "Kp = Kc * (R * T) ^ dn",
            mapOf("Kp" to "Pressure equilibrium constant", "Kc" to "Molar concentration constant", "T" to "Temperature (K)", "dn" to "Change in moles of gas (products - reactants)", "R" to "Gas Constant = 0.08206"),
            "Unites pressure gas-phase equilibrium constants with molar liquid concentration constants."
        ),
        
        // Electrochemistry
        FormulaRecord(
            "Electrochemistry",
            "Nernst Cell Potential (298K)",
            "E = Eo - (0.0592 / n) * log10(Q)",
            mapOf("E" to "Cell Potential (V)", "Eo" to "Standard Cell Potential (V)", "n" to "Number of electrons transferred", "Q" to "Reaction Quotient"),
            "Calculates non-standard electrode potentials utilizing species concentration variables."
        ),
        FormulaRecord(
            "Electrochemistry",
            "Faraday's Mass Deposition",
            "m = (I * t * M) / (z * F)",
            mapOf("m" to "Mass deposited (g)", "I" to "Electric current (A)", "t" to "Total time (s)", "M" to "Molar Mass (g/mol)", "z" to "Valency of metal ion", "F" to "Faraday Constant = 96485 C/mol"),
            "Correlates chemical mass deposition rate on anodes during galvanic cell electrolysis."
        ),

        // Kinetics
        FormulaRecord(
            "Kinetics",
            "Arrhenius Activation Rate",
            "k1_k2_ratio = ln(k2 / k1) = -Ea / R * (1 / T2 - 1 / T1)",
            mapOf("k2_k1_ratio" to "Value of ln(k2/k1)", "Ea" to "Activation Energy (J/mol)", "T1" to "Initial Temp (K)", "T2" to "Final Temp (K)", "R" to "Constant = 8.314"),
            "Expresses temperature dependence of reaction velocity coefficients."
        ),

        // Colligative Properties
        FormulaRecord(
            "Colligative Properties",
            "Osmotic Pressure",
            "pi_op = i * M * R * T",
            mapOf("pi_op" to "Osmotic pressure (atm)", "i" to "Van 't Hoff factor", "M" to "Molarity (mol/L)", "T" to "Temperature (K)", "R" to "Gas constant = 0.08206"),
            "The pressure required to stop the precise trans-membrane osmotic flow of pure water."
        ),
        FormulaRecord(
            "Colligative Properties",
            "Boiling Point Elevation",
            "dTb = i * Kb * molality",
            mapOf("dTb" to "Elevation in boiling point (C)", "i" to "Van 't Hoff factor", "Kb" to "Ebullioscopic constant (C*kg/mol)", "molality" to "Molality (mol/kg)"),
            "Solution vapor-pressure suppression raises the thermal threshold of physical boiling."
        ),
        FormulaRecord(
            "Colligative Properties",
            "Freezing Point Depression",
            "dTf = i * Kf * molality",
            mapOf("dTf" to "Depression in freezing point (C)", "i" to "Van 't Hoff factor", "Kf" to "Cryoscopic constant (C*kg/mol)", "molality" to "Molality (mol/kg)"),
            "Dissolving chemical solute blocks molecular crystallizing structure, depressing freezing temp."
        )
    )
}
