package com.diamon.utilidades;

public class HexInt {
    private int value;

    public HexInt(int value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value >= 0 ? Integer.toHexString(value) : "-" + Integer.toHexString(-value);
    }

    public static int maybeHexInt(Object value) {
        if (value instanceof Integer) {
            return (Integer) value;
        }
        return 0; // Maneja casos de error según sea necesario.
    }

    public static int[] indexwiseAnd(int[] fuses, int[][] settingValues) {

        int[] result = fuses.clone();

        for (int[] setting : settingValues) {

            int index = setting[0];

            int value = setting[1];

            result[index] &= value;
        }
        return result;
    }
}
