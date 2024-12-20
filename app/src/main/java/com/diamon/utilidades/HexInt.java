package com.diamon.utilidades;

public class HexInt {
		private final int value;

		public HexInt(int value) {
			this.value = value;
		}

		@Override
		public String toString() {
			if (value >= 0) {
				return "0x" + Integer.toHexString(value).toUpperCase();
			} else {
				return "-0x" + Integer.toHexString(-value).toUpperCase();
			}
		}

		public int getValue() {
			return value;
		}
	}