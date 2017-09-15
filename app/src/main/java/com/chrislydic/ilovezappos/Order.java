package com.chrislydic.ilovezappos;

/**
 * An entry in an order book.
 */
public class Order {
	private double bitcoinPrice;
	private double amount;
	private double value;

	public Order( double bitcoinPrice, double amount ) {
		this.bitcoinPrice = bitcoinPrice;
		this.amount = amount;
		this.value = bitcoinPrice * amount;
	}

	public double getBitcoinPrice() {
		return bitcoinPrice;
	}

	public double getAmount() {
		return amount;
	}

	public double getValue() {
		return value;
	}
}
