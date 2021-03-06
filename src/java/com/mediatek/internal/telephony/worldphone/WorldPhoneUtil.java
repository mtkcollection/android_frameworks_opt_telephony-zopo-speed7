/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */
package com.mediatek.internal.telephony.worldphone;

import android.content.Context;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.RadioAccessFamily;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneBase;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.ProxyController;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.PhoneProxy;
import com.android.internal.telephony.TelephonyProperties;
import com.mediatek.internal.telephony.cdma.CdmaFeatureOptionUtils;

import com.mediatek.internal.telephony.ltedc.svlte.SvlteModeController;

/**
 *@hide
 */
public class WorldPhoneUtil implements IWorldPhone {
    private static final int PROJECT_SIM_NUM = TelephonyManager.getDefault().getSimCount();
    private static final boolean IS_WORLD_PHONE_SUPPORT = (SystemProperties.getInt("ro.mtk_world_phone", 0) == 1);
    private static final boolean IS_LTE_SUPPORT = (SystemProperties.getInt("ro.mtk_lte_support", 0) == 1);
    private static final boolean IS_CDMA_LTE_DC_SUPPORT = CdmaFeatureOptionUtils.isCdmaLteDcSupport();
    private static final String PROPERTY_MAJOR_SIM = "persist.radio.simswitch";
    private static Context sContext = null;
    private static Phone sDefultPhone = null;
    private static Phone[] sProxyPhones = null;
    private static Phone[] sActivePhones = new Phone[PROJECT_SIM_NUM];

    public WorldPhoneUtil() {
        logd("Constructor invoked");
        sDefultPhone = PhoneFactory.getDefaultPhone();
        sProxyPhones = PhoneFactory.getPhones();
        for (int i = 0; i < PROJECT_SIM_NUM; i++) {
            sActivePhones[i] = ((PhoneProxy) sProxyPhones[i]).getActivePhone();
        }
        if (sDefultPhone != null) {
            sContext = sDefultPhone.getContext();
        } else {
            logd("DefaultPhone = null");
        }
    }

    public static int getProjectSimNum() {
        return PROJECT_SIM_NUM;
    }

    public static int getMajorSim() {
        if (!ProxyController.getInstance().isCapabilitySwitching()) {
            String currMajorSim = SystemProperties.get(PROPERTY_MAJOR_SIM, "");
            if (currMajorSim != null && !currMajorSim.equals("")) {
                logd("[getMajorSim]: " + ((Integer.parseInt(currMajorSim)) - 1));
                return (Integer.parseInt(currMajorSim)) - 1;
            } else {
                logd("[getMajorSim]: fail to get major SIM");
                return MAJOR_SIM_UNKNOWN;
            }
        } else {
            logd("[getMajorSim]: radio capability is switching");
            return MAJOR_SIM_UNKNOWN;
        }
    }

    public static int getModemSelectionMode() {
        if (sContext == null) {
            logd("sContext = null");
            return SELECTION_MODE_AUTO;
        }

        return Settings.Global.getInt(sContext.getContentResolver(),
                    Settings.Global.WORLD_PHONE_AUTO_SELECT_MODE, SELECTION_MODE_AUTO);
    }

    public static boolean isWorldPhoneSupport() {
        return IS_WORLD_PHONE_SUPPORT;
    }

    public static boolean isLteSupport() {
        return IS_LTE_SUPPORT;
    }

    public static String regionToString(int region) {
        String regionString;
        switch (region) {
            case REGION_UNKNOWN:
                regionString = "REGION_UNKNOWN";
                break;
            case REGION_DOMESTIC:
                regionString = "REGION_DOMESTIC";
                break;
            case REGION_FOREIGN:
                regionString = "REGION_FOREIGN";
                break;
            default:
                regionString = "Invalid Region";
                break;
        }

        return regionString;
    }

    public static String stateToString(int state) {
        String stateString;
        switch (state) {
            case ServiceState.STATE_POWER_OFF:
                stateString = "STATE_POWER_OFF";
                break;
            case ServiceState.STATE_IN_SERVICE:
                stateString = "STATE_IN_SERVICE";
                break;
            case ServiceState.STATE_OUT_OF_SERVICE:
                stateString = "STATE_OUT_OF_SERVICE";
                break;
            case ServiceState.STATE_EMERGENCY_ONLY:
                stateString = "STATE_EMERGENCY_ONLY";
                break;
            default:
                stateString = "Invalid State";
                break;
        }

        return stateString;
    }

    public static String regStateToString(int regState) {
        String rsString;
        switch (regState) {
            case ServiceState.REGISTRATION_STATE_NOT_REGISTERED_AND_NOT_SEARCHING:
                rsString = "REGISTRATION_STATE_NOT_REGISTERED_AND_NOT_SEARCHING";
                break;
            case ServiceState.REGISTRATION_STATE_HOME_NETWORK:
                rsString = "REGISTRATION_STATE_HOME_NETWORK";
                break;
            case ServiceState.REGISTRATION_STATE_NOT_REGISTERED_AND_SEARCHING:
                rsString = "REGISTRATION_STATE_NOT_REGISTERED_AND_SEARCHING";
                break;
            case ServiceState.REGISTRATION_STATE_REGISTRATION_DENIED:
                rsString = "REGISTRATION_STATE_REGISTRATION_DENIED";
                break;
            case ServiceState.REGISTRATION_STATE_UNKNOWN:
                rsString = "REGISTRATION_STATE_UNKNOWN";
                break;
            case ServiceState.REGISTRATION_STATE_ROAMING:
                rsString = "REGISTRATION_STATE_ROAMING";
                break;
            default:
                rsString = "Invalid RegState";
                break;
        }

        return rsString;
    }

    public static String denyReasonToString(int reason) {
        String drString;
        switch (reason) {
            case CAMP_ON_NOT_DENIED:
                drString = "CAMP_ON_NOT_DENIED";
                break;
            case CAMP_ON_DENY_REASON_UNKNOWN:
                drString = "CAMP_ON_DENY_REASON_UNKNOWN";
                break;
            case CAMP_ON_DENY_REASON_NEED_SWITCH_TO_FDD:
                drString = "CAMP_ON_DENY_REASON_NEED_SWITCH_TO_FDD";
                break;
            case CAMP_ON_DENY_REASON_NEED_SWITCH_TO_TDD:
                drString = "CAMP_ON_DENY_REASON_NEED_SWITCH_TO_TDD";
                break;
            case CAMP_ON_DENY_REASON_DOMESTIC_FDD_MD:
                drString = "CAMP_ON_DENY_REASON_DOMESTIC_FDD_MD";
                break;
            default:
                drString = "Invalid Reason";
                break;
        }

        return drString;
    }

    public static String iccCardTypeToString(int iccCardType) {
        String iccTypeString;
        switch (iccCardType) {
            case ICC_CARD_TYPE_SIM:
                iccTypeString = "SIM";
                break;
            case ICC_CARD_TYPE_USIM:
                iccTypeString = "USIM";
                break;
            case ICC_CARD_TYPE_UNKNOWN:
                iccTypeString = "Icc Card Type Unknown";
                break;
            default:
                iccTypeString = "Invalid Icc Card Type";
                break;
        }

        return iccTypeString;
    }

    public void setModemSelectionMode(int mode, int modemType) {
    }

    public void notifyRadioCapabilityChange(int capailitySimId) {
    }

    public static boolean isCdmaLteDcSupport(){
        return IS_CDMA_LTE_DC_SUPPORT;
    }

    //C2K world phone
    public static int getRadioTechModeForWp(){
        int mode = RADIO_TECH_MODE_FOR_WP_UNKNOWN;
        if (isCdmaLteDcSupport()) {
            int majorySimId = getMajorSim();
            int svlteModeSlotId = SvlteModeController.getActiveSvlteModeSlotId();
            logd("[getRadioTechModeForWp]: majorySimId=" + majorySimId +
                    " svlteModeSlotId=" + svlteModeSlotId);
            if (majorySimId != MAJOR_SIM_UNKNOWN){
                if (svlteModeSlotId == majorySimId) {
                    mode = RADIO_TECH_MODE_FOR_WP_SVLTE;
                } else {
                    mode = RADIO_TECH_MODE_FOR_WP_CSFB;
                }
            } else {
                mode = RADIO_TECH_MODE_FOR_WP_UNKNOWN;
            }
        } else {
            mode = RADIO_TECH_MODE_FOR_WP_CSFB;
        }
        logd("[getRadioTechModeForWp]: "+ mode);
        return mode;
    }

    private static void logd(String msg) {
        Rlog.d(LOG_TAG, "[WPP_UTIL]" + msg);
    }
}
