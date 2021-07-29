package com.wilbert.svcamera.metering;

/**
 * @author wilbert
 * @Date 2020/12/22 18:33
 * @email jw20082009@qq.com
 **/
public enum ManualType {
    TYPE_MANUAL_SMART  // 手动点击测光后会锁定测光区域，当锁定区域画面有大量变动时解除锁定回到中心/人脸测光
    ,TYPE_MANUAL_LOCK; // 手动点击测光后会锁定测光区域，无法通过扰动锁定区域解除锁定，要想改变测光位置只能再次手动测光
}