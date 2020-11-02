package com.xuershangda.joystick.utils;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Double的精确的四则运算，底层使用BigDecimal实现。默认支持最大34有效数字<br>
 * Long或Integer与Double的乘除，现在先不提供，到时候需要再提供吧。可以使用Long.doubleValue()
 * 或者Integer.doubleValue()转成double，然后进行计算，利用Java的自动装箱和拆箱。<br>
 * 枚举常量摘要 ：<br>
 * BigDecimal.ROUND_UP 向上取整<br>
 * BigDecimal.ROUND_DOWN 向下取整（截断）<br>
 * 
 * BigDecimal.ROUND_HALF_DOWN 五舍六入<br>
 * BigDecimal.ROUND_HALF_EVEN 四舍六入，逢五奇进偶舍<br>
 * BigDecimal.ROUND_HALF_UP 四舍五入<br>
 * 
 * BigDecimal.ROUND_FLOOR 负的向上取整同ROUND_UP；正的直接截断，同ROUND_DOWN。和ROUND_CEILING刚好相反<br>
 * BigDecimal.ROUND_CEILING 正的向上取整同ROUND_UP；负的直接截断，同ROUND_DOWN。和ROUND_FLOOR刚好相反<br>
 * BigDecimal.ROUND_UNNECESSARY 不进行舍入操作，如果指定的精度导致数字长度需要截断，那么抛出异常<br>
 * 总结：
 * 1：不要使用double构造BigDecimal，使用int或String类型。<br>
 * 2：做乘除计算时，一定要设置精度和保留小数点位数。<br>
 * 3：建议BigDecimal计算时，单独放到try catch内<br>
 * 
 * @author yinlei
 * date 2012-6-3 上午10:32:18
 */
public class BigDecimalUtils {

	// 默认除法运算精度
	private static final int DEFAULT_DIV_SCALE = 6;

	/**
	 * 提供精确的加法运算，v1+v2。最大34位有效数字，四舍六入、逢5奇进偶舍。
	 * @param v1 被加数
	 * @param v2 加数
	 * @return 两个参数的和
	 */
	public static double add(Double v1, Double v2) {
		BigDecimal b1 = new BigDecimal(v1.toString());
		BigDecimal b2 = new BigDecimal(v2.toString());
		return b1.add(b2, MathContext.DECIMAL128).doubleValue();
	}
	
	/**
	 * 提供精确的加法运算，v1+v2。
	 * @param v1 被加数
	 * @param augend 加数
	 * @param context 计算上下文 new MathContext(precision, roundingMode) 第一个参数有效数字，
	 * 		第二个参数舍入模式{@link RoundingMode}，可以参见类注释。
	 * @return 两个参数的和
	 */
	public static double add(Double v1, Double augend, MathContext context) {
		BigDecimal b1 = new BigDecimal(v1.toString());
		BigDecimal b2 = new BigDecimal(augend.toString());
		return b1.add(b2, context).doubleValue();
	}
	
	/**
	 * 提供精确的加法运算，v1+v2。
	 * @param v1 被加数
	 * @param v2 加数
	 * @param precision 有效数字位数
	 * @param mode 舍入模式，见类注释
	 * @return 两个参数的和
	 */
	public static double add(Double v1, Double v2, int precision, RoundingMode mode) {
		BigDecimal b1 = new BigDecimal(v1.toString());
		BigDecimal b2 = new BigDecimal(v2.toString());
		MathContext context = new MathContext(precision, mode);
		return b1.add(b2, context).doubleValue();
	}
	
	/**
	 * 提供精确的减法运算，v1-v2。最大34位有效数字，四舍六入、逢5奇进偶舍。
	 * @param v1 被减数
	 * @param subtrahend 减数
	 * @return 两个参数的差
	 */
	public static double subtract(Double v1, Double subtrahend) {
		BigDecimal b1 = new BigDecimal(v1.toString());
		BigDecimal b2 = new BigDecimal(subtrahend.toString());
		return b1.subtract(b2, MathContext.DECIMAL128).doubleValue();
	}
	
	/**
	 * 提供精确的减法运算，v1-v2。
	 * @param v1 被减数
	 * @param subtrahend 减数
	 * @param context 计算上下文 new MathContext(precision, roundingMode) 第一个参数有效数字，
	 * 		第二个参数舍入模式{@link RoundingMode}，可以参见类注释。
	 * @return 两个参数的差
	 */
	public static double subtract(Double v1, Double subtrahend, MathContext context) {
		BigDecimal b1 = new BigDecimal(v1.toString());
		BigDecimal b2 = new BigDecimal(subtrahend.toString());
		return b1.subtract(b2, context).doubleValue();
	}
	
	/**
	 * 提供精确的减法运算，v1-v2。
	 * @param v1 被减数
	 * @param subtrahend 减数
	 * @param precision 有效数字位数
	 * @param mode 舍入模式，可以参见类注释。
	 * @return 两个参数的差
	 */
	public static double subtract(Double v1, Double subtrahend, int precision, RoundingMode mode) {
		BigDecimal b1 = new BigDecimal(v1.toString());
		BigDecimal b2 = new BigDecimal(subtrahend.toString());
		MathContext context = new MathContext(precision, mode);
		return b1.subtract(b2, context).doubleValue();
	}
	
	/**
	 * 提供精确的乘法运算，v1 x v2。最大34位有效数字，四舍六入、逢5奇进偶舍。
	 * @param v1 被乘数
	 * @param v2 乘数
	 * @return 两个参数的积
	 */
	public static double multiply(Double v1, Double v2) {
		BigDecimal b1 = new BigDecimal(v1.toString());
		BigDecimal b2 = new BigDecimal(v2.toString());
		return b1.multiply(b2, MathContext.DECIMAL128).doubleValue();
	}
	
	/**
	 * 提供精确的乘法运算，v1 x v2。最大34位有效数字，四舍六入、逢5奇进偶舍。
	 * @param v1 被乘数
	 * @param v2 乘数
	 * @param scale 精确到小数点以后几位，非负整数
	 * @return 两个参数的积
	 */
	public static double multiply(Double v1, Double v2, int scale) {
		BigDecimal b1 = new BigDecimal(v1.toString());
		BigDecimal b2 = new BigDecimal(v2.toString());
		return b1.multiply(b2, MathContext.DECIMAL128).setScale(scale, BigDecimal.ROUND_HALF_UP).doubleValue();
	}
	
	/**
	 * 提供精确的乘法运算，v1xv2。
	 * @param v1 被乘数
	 * @param multiplicand 乘数
	 * @param context 计算上下文 new MathContext(precision, roundingMode) 第一个参数有效数字，
	 * 		第二个参数舍入模式{@link RoundingMode}，可以参见类注释。
	 * @return 两个参数的积
	 */
	public static double multiply(Double v1, Double multiplicand, MathContext context) {
		BigDecimal b1 = new BigDecimal(v1.toString());
		BigDecimal b2 = new BigDecimal(multiplicand.toString());
		return b1.multiply(b2, context).doubleValue();
	}
	
	/**
	 * 提供精确的乘法运算，v1xv2。
	 * @param v1 被乘数
	 * @param multiplicand 乘数
	 * @param context 计算上下文 new MathContext(precision, roundingMode) 第一个参数有效数字，
	 * 		第二个参数舍入模式{@link RoundingMode}，可以参见类注释。
	 * @param scale 精确到小数点以后几位，非负整数
	 * @return 两个参数的积
	 */
	public static double multiply(Double v1, Double multiplicand, int scale, MathContext context) {
		BigDecimal b1 = new BigDecimal(v1.toString());
		BigDecimal b2 = new BigDecimal(multiplicand.toString());
		return b1.multiply(b2, context).setScale(scale).doubleValue();
	}
	
	/**
	 * 提供精确的乘法运算，v1 x v2。
	 * @param v1 被乘数
	 * @param multiplicand 乘数
	 * @param precision 有效数字位数
	 * @param mode 舍入模式，可以参见类注释。
	 * @return 两个参数的积
	 */
	public static double multiply(Double v1, Double multiplicand, int precision, RoundingMode mode) {
		BigDecimal b1 = new BigDecimal(v1.toString());
		BigDecimal b2 = new BigDecimal(multiplicand.toString());
		MathContext context = new MathContext(precision, mode);
		return b1.multiply(b2, context).doubleValue();
	}
	
	/**
	 * 提供（相对）精确的除法运算，当发生除不尽的情况时，精确到小数点以后6位，以后的数字四舍五入。
	 * @param dividend 被除数
	 * @param divisor 除数
	 * @return 两个参数的商
	 */
	public static double divide(Double dividend, Double divisor) {
		return divide(dividend, divisor, DEFAULT_DIV_SCALE);
	}
	
	/**
	 * 提供（相对）精确的除法运算。当发生除不尽的情况时，由scale参数指定精度，以后的数字四舍五入。
	 * @param dividend 被除数
	 * @param divisor 除数
	 * @param scale 表示需要精确到小数点以后几位，非负整数
	 * @return 两个参数的商
	 */
	public static double divide(Double dividend, Double divisor, int scale) {
		if (scale < 0) {
			throw new IllegalArgumentException("The scale 必须是非负整数。");
		}
		BigDecimal b1 = new BigDecimal(dividend.toString());
		BigDecimal b2 = new BigDecimal(divisor.toString());
		return b1.divide(b2, scale, BigDecimal.ROUND_HALF_UP).doubleValue();
	}
	
	/**
	 * 提供（相对）精确的除法运算。当发生除不尽的情况时，由scale参数指定精度，由roundMode参数指定的模式进行舍入。<br>
	 * 建议使用 {@link #divide(Double, Double, int, int)}
	 * @param dividend 被除数
	 * @param divisor 除数
	 * @param scale 表示需要精确到小数点以后几位，非负整数
	 * @param roundingMode 舍入模式，非负整数，BigDecimal.ROUND_HALF_UP有类似常量
	 * @return 两个参数的商
	 */
	public static double divide(Double dividend, Double divisor, int scale, int roundingMode) {
		if (scale < 0 || roundingMode < 0) {
			throw new IllegalArgumentException("The scale or roundingMode 必须是非负整数");
		}
		BigDecimal b1 = new BigDecimal(dividend.toString());
		BigDecimal b2 = new BigDecimal(divisor.toString());
		return b1.divide(b2, scale, roundingMode).doubleValue();
	}
	
	/**
	 * 提供（相对）精确的除法运算。当发生除不尽的情况时，由scale参数指定精度，由roundMode参数指定的模式进行舍入。
	 * @param dividend 被除数
	 * @param divisor 除数
	 * @param scale 表示需要精确到小数点以后几位，非负整数
	 * @param roundingMode 舍入模式，见 {@link RoundingMode}，或类注释
	 * @return 两个参数的商
	 */
	public static double divide(Double dividend, Double divisor, int scale, RoundingMode roundingMode) {
		if (scale < 0) {
			throw new IllegalArgumentException("The scale 必须是非负整数");
		}
		BigDecimal b1 = new BigDecimal(dividend.toString());
		BigDecimal b2 = new BigDecimal(divisor.toString());
		return b1.divide(b2, scale, roundingMode).doubleValue();
	}
	
	/**
	 * 提供（相对）精确的除法运算。当发生除不尽的情况时，有效数字和舍入模式由MathContext决定。<br>
	 * 不建议使用这个方法，因为没有提供小数位数的控制。建议使用{@link #divide(Double, Double, int, RoundingMode)}
	 * @param dividend 被除数
	 * @param divisor 除数
	 * @param context 计算上下文
	 * @return 两个参数的商
	 */
	public static double divide(Double dividend, Double divisor, MathContext context) {
		BigDecimal b1 = new BigDecimal(dividend.toString());
		BigDecimal b2 = new BigDecimal(divisor.toString());
		return b1.divide(b2, context).doubleValue();
	}
	
	/**
	 * 提供精确的小数位四舍五入处理。
	 * @param value 需要四舍五入的数字
	 * @param scale 小数点后保留几位，非负整数
	 * @return 四舍五入后的结果
	 */
	public static double round(Double value, int scale) {
		if (scale < 0) {
			throw new IllegalArgumentException("The scale 必须是非负整数。");
		}
		BigDecimal b = new BigDecimal(value.toString());
		return b.divide(BigDecimal.ONE, scale, BigDecimal.ROUND_HALF_UP).doubleValue();
	}
	
	/**
	 * 提供精确的小数位舍入处理。建议使用这个{@link #round(Double, int, RoundingMode)}
	 * @param value 需要舍入的数字
	 * @param scale 小数点后保留几位，非负整数
	 * @param roundingMode 舍入模式，见 {@link RoundingMode}，或类注释。
	 * @return 舍入后的结果
	 */
	public static double round(Double value, int scale, int roundingMode) {
		if (scale < 0 || roundingMode < 0) {
			throw new IllegalArgumentException("The scale or roundingMode 必须是非负整数。");
		}
		BigDecimal b = new BigDecimal(value.toString());
		return b.divide(BigDecimal.ONE, scale, roundingMode).doubleValue();
	}
	
	/**
	 * 提供精确的小数位舍入处理。
	 * @param value 需要舍入的数字
	 * @param scale 小数点后保留几位，非负整数
	 * @param roundingMode 舍入模式，见 {@link RoundingMode}，或类注释。使用枚举，防止乱传参数。这个枚举其实是int类型的。
	 * @return 舍入后的结果
	 */
	public static double round(Double value, int scale, RoundingMode roundingMode) {
		if (scale < 0) {
			throw new IllegalArgumentException("The scale 必须是非负整数。");
		}
		BigDecimal b = new BigDecimal(value.toString());
		return b.divide(BigDecimal.ONE, scale, roundingMode).doubleValue();
	}

}
