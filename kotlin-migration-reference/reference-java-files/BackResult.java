package com.uhf.event;


/**
 * author CYD
 * date 2018/11/19
 *
 */
public interface BackResult extends OnKeyDownListener {
    void postResult(String[] tagData);

    void postInventoryRate(long rate);
}
