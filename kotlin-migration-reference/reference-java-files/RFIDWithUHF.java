package com.uhf.uhfdemo;

/**
 * author CYD
 * date 2018/11/19
 * email cyd19950902@qq.com
 */
public class RFIDWithUHF {

    public static enum BankEnum {
        RESERVED((byte)0),
        UII((byte)1),
        TID((byte)2),
        USER((byte)3);

        private final byte a;

        public final byte getValue() {
            return this.a;
        }

        private BankEnum(byte value) {
            this.a = value;
        }
    }
}
