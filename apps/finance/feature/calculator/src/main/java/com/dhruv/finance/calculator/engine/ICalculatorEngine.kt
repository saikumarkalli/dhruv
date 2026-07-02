package com.dhruv.finance.calculator.engine

interface ICalculatorEngine {
    /**
     * Evaluates a mathematical expression string.
     * @param expression The mathematical expression to compute.
     * @param isDegree Set to true to evaluate trigonometric functions in degrees, false for radians.
     * @return The double value result of the calculation.
     * @throws ArithmeticException under domain violations (division by zero, negative square root, log of non-positive).
     * @throws RuntimeException under syntax or parsing errors.
     */
    fun evaluate(
        expression: String,
        isDegree: Boolean,
    ): Double

    /**
     * Computes the factorial of a number.
     * @param n The input value.
     * @return The factorial of the value, Double.NaN for decimals or negative numbers, or Double.POSITIVE_INFINITY for numbers > 170.
     */
    fun factorial(n: Double): Double
}
