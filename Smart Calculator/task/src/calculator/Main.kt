package calculator

import java.math.BigInteger

val appVar = mutableMapOf<String, BigInteger>()

val calculatorExpReg = """[(]?(([+-]?\d+)|[a-zA-Z]+)(\s*(([+-]+)|([*^/]))\s*[(]*(([+-]?\d+)|[a-zA-Z]+)[)]*)*[)]?""".toRegex()
val calculatorCmdReg = """/\w*""".toRegex()
val varReg = """[a-zA-Z]+""".toRegex()
val assignmentExpReg = """\w+(\s*=\s*(\w+|([+-]?\d+)))+""".toRegex()
val assignmentOpReg = """\s*=\s*""".toRegex()
val intExpReg = """[+-]?\d+""".toRegex()
val operatorExpReg = """[-+/^*()]""".toRegex()

const val openParenthesis = '('
const val closeParenthesis = ')'


enum class Operator(val representation: Char, val priorityLevel: Int) {
    ADD('+', 1), SUB('-', 1), TIME('*', 2), DIV('/', 2), POWER('^', 3)
}

sealed interface CalculatorFinalResult {
    operator fun invoke()
}

object Finish : CalculatorFinalResult {
    override operator fun invoke() {
        println("Bye!")
    }
}

data class CalculationDone(val result: String) : CalculatorFinalResult {
    override operator fun invoke() {
        println(result)
    }
}

object InvalidIdentifier : CalculatorFinalResult {
    override operator fun invoke() {
        println("Invalid identifier")
    }
}

object InvalidAssignment : CalculatorFinalResult {
    override operator fun invoke() {
        println("Invalid assignment")
    }
}

object UnknownVariable : CalculatorFinalResult {
    override operator fun invoke() {
        println("Unknown variable")
    }
}

object InvalidExpression : CalculatorFinalResult {
    override operator fun invoke() {
        println("Invalid expression")
    }
}

object UnknownCommand : CalculatorFinalResult {
    override operator fun invoke() {
        println("Unknown command")
    }
}

fun String.addSubAccumulatorResolution(): Char = fold('+') { init, current ->
    if (init == '+') {
        current
    } else {
        if (current == '+') init
        else '+'
    }
}

fun executeOp(left: BigInteger, operation: Operator, right: BigInteger): BigInteger = when (operation) {
    Operator.ADD -> left + right
    Operator.SUB -> left - right
    Operator.TIME -> left * right
    Operator.DIV -> left / right
    Operator.POWER -> left.pow(right.toInt())
}

fun handleAssignationCases(input: String): CalculatorFinalResult? {
    val (left, right) = input.split(assignmentOpReg, 2)
    when {
        !varReg.matches(left) -> {
            return InvalidIdentifier
        }

        intExpReg.matches(right) -> {
            appVar[left] = right.toBigInteger()
        }

        !intExpReg.matches(right) -> {
            if (right !in appVar.keys) {
                return if (varReg.matches(right)) {
                    UnknownVariable
                } else {
                    InvalidAssignment
                }
            } else {
                appVar[left] = appVar[right]!!
            }
        }
    }
    return null
}

fun sanitiseInputByOperator(input: String): String = input.replace("""\s+""".toRegex(), "").replace("""[+-]{2,}""".toRegex()) {
    it.value.addSubAccumulatorResolution().toString()
}

fun toDoOnCloseParenthesisInPostfixConstruction(postFixFormat: MutableList<String>, operatorStack: MutableList<String>): CalculatorFinalResult? {
    if (openParenthesis.toString() !in operatorStack) return InvalidExpression
    while (operatorStack.isNotEmpty()) {
        val latestOperatorStackedRepresentation = operatorStack.removeLast()
        if (latestOperatorStackedRepresentation == closeParenthesis.toString()) return InvalidExpression
        (latestOperatorStackedRepresentation == openParenthesis.toString()).also {
            if (it) return null
            else postFixFormat.add(latestOperatorStackedRepresentation)
        }
    }
    return InvalidExpression
}

fun canPushOperatorToStack(currentCharOperator: Char, operatorStack: List<String>): Boolean =
        currentCharOperator == openParenthesis ||
                operatorStack.isEmpty() ||
                with(operatorStack.last()) {
                    this == openParenthesis.toString() ||
                            Operator.values().run {
                                val latestOperatorStacked = first { it.representation.toString() == this@with }
                                val currentOperator = first { it.representation.toString() == currentCharOperator.toString() }
                                latestOperatorStacked.priorityLevel < currentOperator.priorityLevel
                            }
                }

fun computePostfixExpression(postFixFormat: List<String>): String {
    val resultStack = mutableListOf<String>()
    postFixFormat.forEach { element ->
        if (operatorExpReg.matches(element)) {
            val rightOperand = resultStack.removeLast().toBigInteger()
            if (resultStack.isNotEmpty()) {
                val leftOperand = resultStack.removeLast().toBigInteger()
                resultStack.add(executeOp(leftOperand, Operator.values().first { it.representation.toString() == element }, rightOperand).toString())
            } else resultStack.add("$element$rightOperand".toBigInteger().toString())
        } else {
            resultStack.add(element)
        }
    }
    return resultStack.last()
}

fun computeInput(input: String): CalculatorFinalResult {
    val sanitisedInputByOperator = sanitiseInputByOperator(input)
    val operatorStack = mutableListOf<String>()
    val postFixFormat = mutableListOf<String>()
    var remainingExpressionProcessing = sanitisedInputByOperator
    do {
        val startChar = remainingExpressionProcessing.first()
        if (operatorExpReg.matches(startChar.toString())) {
            if (startChar == closeParenthesis) {
                toDoOnCloseParenthesisInPostfixConstruction(postFixFormat, operatorStack)?.also {
                    return it
                }
            } else if (canPushOperatorToStack(startChar, operatorStack)) {
                operatorStack.add(startChar.toString())
            } else {
                val currentOperator = Operator.values().first { it.representation.toString() == startChar.toString() }
                do {
                    val latestOperatorStackedRepresentation = operatorStack.last()
                    val canPushOperatorToStack = ((latestOperatorStackedRepresentation == openParenthesis.toString())
                            || Operator.values().first { it.representation.toString() == latestOperatorStackedRepresentation }.priorityLevel < currentOperator.priorityLevel)
                    if (canPushOperatorToStack) {
                        break
                    } else {
                        operatorStack.removeLast()
                        postFixFormat.add(latestOperatorStackedRepresentation)
                    }
                } while (operatorStack.isNotEmpty())
                operatorStack.add(startChar.toString())
            }
            remainingExpressionProcessing = remainingExpressionProcessing.substringAfter(startChar, "")
        } else {
            val expressionStart = remainingExpressionProcessing.substringBefore(operatorExpReg.find(remainingExpressionProcessing)?.value
                    ?: " ")
            if (!intExpReg.matches(expressionStart)) {
                if (expressionStart !in appVar.keys) {
                    return if (varReg.matches(expressionStart)) {
                        UnknownVariable
                    } else {
                        InvalidIdentifier
                    }
                } else {
                    postFixFormat.add(appVar[expressionStart]!!.toString())
                }
            } else {
                postFixFormat.add(expressionStart)
            }
            remainingExpressionProcessing = remainingExpressionProcessing.substringAfter(expressionStart, "")
        }
    } while (remainingExpressionProcessing.isNotEmpty())

    if (openParenthesis.toString() in operatorStack || closeParenthesis.toString() in operatorStack) return InvalidExpression
    postFixFormat.addAll(operatorStack.reversed())
    return CalculationDone(computePostfixExpression(postFixFormat))
}

fun main() {
    do {
        val input = readln().trim()
        if (input.isBlank()) continue
        if (calculatorCmdReg.matches(input)) {
            if (input.contentEquals("/exit")) break
            else if (input.contentEquals("/help")) println("The program calculates the sum of numbers")
            else UnknownCommand()
        } else if (input in appVar.keys) {
            println(appVar[input])
        } else if (assignmentExpReg.matches(input)) {
            handleAssignationCases(input)?.invoke()
        } else if (calculatorExpReg.matches(input)) {
            computeInput(input)()
        } else {
            if (varReg.matches(input)) UnknownVariable()
            else {
                if (input.contains("""[/^*]{2,}""".toRegex())) InvalidExpression()
                else InvalidIdentifier()
            }
        }
    } while (true)
    Finish()
}
