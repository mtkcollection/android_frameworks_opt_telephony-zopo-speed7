package com.mediatek.internal.telephony.ltedc.svlte;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Build;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.CellInfoCdma;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.util.EventLog;

import com.android.internal.telephony.EventLogTags;
import com.android.internal.telephony.MccTable;
import com.android.internal.telephony.TelephonyProperties;
import com.android.internal.telephony.cdma.CDMAPhone;
import com.android.internal.telephony.cdma.CdmaServiceStateTracker;


/**
 * Add for SVLTE CDMA ServiceStateTracker to notify the gsm and cdma related state change.
 */
public class SvlteServiceStateTracker extends CdmaServiceStateTracker {
    private static final String LOG_TAG = "SvlteSST";
    private static final boolean DBG = true;
    private SignalStrength mGSMSignalStrength = new SignalStrength();
    private SignalStrength mCDMASignalStrength = new SignalStrength(false);
    private SignalStrength mCombinedSignalStrength = new SignalStrength(false);
    private ServiceState mGSMSS = new ServiceState();
    private ServiceState mCDMASS = new ServiceState();
    private ServiceState mCombinedSS = new ServiceState();

    /**
     * The Service State Tracker for SVLTE.
     * @param phone The CDMAPhone to create the Servie State Tracker.
     */
    public SvlteServiceStateTracker(CDMAPhone phone) {
        super(phone, new CellInfoCdma());
    }

    /**
     * send gsm signal-strength-changed notification if changed Called both for
     * solicited and unsolicited signal strength updates.
     * @param gsmSignalStrength The gsm Signal Strength
     * @return true if the gsm signal strength changed and a notification was
     *         sent.
     */
    public boolean onGSMSignalStrengthResult(SignalStrength gsmSignalStrength) {
        log("onGSMSignalStrengthResult(): gsmSignalStrength = "
                + gsmSignalStrength.toString());
        mGSMSignalStrength = new SignalStrength(gsmSignalStrength);
        combineGsmCdmaSignalStrength();
        return notifySignalStrength();
    }

    /**
     * send cdma signal-strength-changed notification if changed Called both for
     * solicited and unsolicited signal strength updates.
     * @param cdmaSignalStrength The cdma Signal Strength
     * @return true if the cdma signal strength changed and a notification was
     *         sent.
     */
    public boolean onCDMASignalStrengthResult(SignalStrength cdmaSignalStrength) {
        log("onCDMASignalStrengthResult(): cdmaSignalStrength = "
                + cdmaSignalStrength.toString());
        mCDMASignalStrength = new SignalStrength(cdmaSignalStrength);
        combineGsmCdmaSignalStrength();
        return notifySignalStrength();
    }

    protected SignalStrength mLastCombinedSignalStrength = null;

    protected boolean notifySignalStrength() {
        boolean notified = false;
        synchronized (mCellInfo) {
            if (!mCombinedSignalStrength.equals(mLastCombinedSignalStrength)) {
                try {
                    if (DBG) {
                        log("notifySignalStrength: mCombinedSignalStrength.getLevel="
                                + mCombinedSignalStrength.getLevel());
                    }
                    mPhone.notifySignalStrength();
                    mLastCombinedSignalStrength = new SignalStrength(
                            mCombinedSignalStrength);
                    notified = true;
                } catch (NullPointerException ex) {
                    loge("updateSignalStrength() Phone already destroyed: "
                            + ex + "SignalStrength not notified");
                }
            }
        }
        return notified;
    }

    private void combineGsmCdmaSignalStrength() {
        if (DBG) {
            log("combineGsmCdmaSignalStrength: mGSMSignalStrength= "
                    + mGSMSignalStrength + "mCDMASignalStrength = "
                    + mCDMASignalStrength);
        }
        mCombinedSignalStrength.setGsmSignalStrength(mGSMSignalStrength
                .getGsmSignalStrength());
        mCombinedSignalStrength.setGsmBitErrorRate(mGSMSignalStrength
                .getGsmBitErrorRate());
        mCombinedSignalStrength.setLteSignalStrength(mGSMSignalStrength
                .getLteSignalStrength());
        mCombinedSignalStrength.setLteRsrp(mGSMSignalStrength.getLteRsrp());
        mCombinedSignalStrength.setLteRsrq(mGSMSignalStrength.getLteRsrq());
        mCombinedSignalStrength.setLteRssnr(mGSMSignalStrength.getLteRssnr());
        mCombinedSignalStrength.setLteCqi(mGSMSignalStrength.getLteCqi());
        mCombinedSignalStrength.setGsmRssiQdbm(mGSMSignalStrength.getGsmRssiQdbm());
        mCombinedSignalStrength.setGsmRscpQdbm(mGSMSignalStrength.getGsmRscpQdbm());
        mCombinedSignalStrength.setGsmEcn0Qdbm(mGSMSignalStrength.getGsmEcn0Qdbm());
        mCombinedSignalStrength.setEvdoDbm(mCDMASignalStrength.getEvdoDbm());
        mCombinedSignalStrength.setEvdoEcio(mCDMASignalStrength.getEvdoEcio());
        mCombinedSignalStrength.setEvdoSnr(mCDMASignalStrength.getEvdoSnr());
        mCombinedSignalStrength.setCdmaDbm(mCDMASignalStrength.getCdmaDbm());
        mCombinedSignalStrength.setCdmaEcio(mCDMASignalStrength.getCdmaEcio());
        if (DBG) {
            log("combineGsmCdmaSignalStrength: mCombinedSignalStrength= "
                    + mCombinedSignalStrength);
        }
    }

    /**
     * @return signal strength
     */
    public SignalStrength getSignalStrength() {
        synchronized (mCellInfo) {
            return mCombinedSignalStrength;
        }
    }

    @Override
    public void handleMessage(Message msg) {

        AsyncResult ar;
        switch (msg.what) {
        case EVENT_SIGNAL_STRENGTH_UPDATE:
            // This is a notification from
            // CommandsInterface.setOnSignalStrengthUpdate.

            ar = (AsyncResult) msg.obj;
            log("EVENT_SIGNAL_STRENGTH_UPDATE, ar = " + ar.result);

            // The radio is telling us about signal strength changes,
            // so we don't have to ask it.
            mDontPollSignalStrength = true;
            setSignalStrength(ar, false);
            onCDMASignalStrengthResult(mSignalStrength);
            break;

        case EVENT_GET_SIGNAL_STRENGTH:
            // This callback is called when signal strength is polled
            // all by itself

            if (!(mCi.getRadioState().isOn())) {
                // Polling will continue when radio turns back on
                return;
            }
            ar = (AsyncResult) msg.obj;
            log("EVENT_GET_SIGNAL_STRENGTH, ar = " + ar.result);
            setSignalStrength(ar, false);
            onCDMASignalStrengthResult(mSignalStrength);
            queueNextSignalStrengthPoll();
            break;

        default:
            super.handleMessage(msg);
            break;
        }
    }

    @Override
    protected void pollStateDone() {
        if (DBG) {
            log(this + "pollStateDone: cdma oldSS=[" + mSS + "] newSS=["
                    + mNewSS + "]");
        }

        if (Build.IS_DEBUGGABLE
                && SystemProperties.getBoolean(PROP_FORCE_ROAMING, false)) {
            mNewSS.setRoaming(true);
        }

        useDataRegStateForDataOnlyDevices();

        boolean hasRegistered = mSS.getVoiceRegState() != ServiceState.STATE_IN_SERVICE
                && mNewSS.getVoiceRegState() == ServiceState.STATE_IN_SERVICE;

        boolean hasDeregistered = mSS.getVoiceRegState() == ServiceState.STATE_IN_SERVICE
                && mNewSS.getVoiceRegState() != ServiceState.STATE_IN_SERVICE;

        boolean hasCdmaDataConnectionAttached =
                mSS.getDataRegState() != ServiceState.STATE_IN_SERVICE
                && mNewSS.getDataRegState() == ServiceState.STATE_IN_SERVICE;

        boolean hasCdmaDataConnectionDetached =
                mSS.getDataRegState() == ServiceState.STATE_IN_SERVICE
                && mNewSS.getDataRegState() != ServiceState.STATE_IN_SERVICE;

        boolean hasCdmaDataConnectionChanged = mSS.getDataRegState() != mNewSS
                .getDataRegState();

        boolean hasRilVoiceRadioTechnologyChanged = mSS
                .getRilVoiceRadioTechnology() != mNewSS
                .getRilVoiceRadioTechnology();

        boolean hasRilDataRadioTechnologyChanged = mSS
                .getRilDataRadioTechnology() != mNewSS
                .getRilDataRadioTechnology();

        boolean hasDataRegStateChanged =
                mSS.getDataRegState() != mNewSS.getDataRegState();

        boolean hasVoiceRegStateChanged =
                mSS.getVoiceRegState() != mNewSS.getVoiceRegState();

        boolean hasChanged = !mNewSS.equals(mSS);

        boolean hasVoiceRoamingOn = !mSS.getVoiceRoaming() && mNewSS.getVoiceRoaming();

        boolean hasVoiceRoamingOff = mSS.getVoiceRoaming() && !mNewSS.getVoiceRoaming();

        boolean hasDataRoamingOn = !mSS.getDataRoaming() && mNewSS.getDataRoaming();

        boolean hasDataRoamingOff = mSS.getDataRoaming() && !mNewSS.getDataRoaming();

        boolean hasLocationChanged = !mNewCellLoc.equals(mCellLoc);

        // / M: c2k modify. @{
        boolean hasRegStateChanged = mSS.getRegState() != mNewSS.getRegState();
        log("pollStateDone: hasRegStateChanged = " + hasRegStateChanged);
        log("pollStateDone: hasRegistered:" + hasRegistered + ",hasDeregistered:" + hasDeregistered
                + ",hasCdmaDataConnectionAttached:" + hasCdmaDataConnectionAttached
                + ",hasCdmaDataConnectionDetached:" + hasCdmaDataConnectionDetached
                + ",hasCdmaDataConnectionChanged:" + hasCdmaDataConnectionChanged
                + ",hasRilVoiceRadioTechnologyChanged:" + hasRilVoiceRadioTechnologyChanged
                + ",hasRilDataRadioTechnologyChanged:" + hasRilDataRadioTechnologyChanged
                + ",hasVoiceRegStateChanged:" + hasVoiceRegStateChanged + ",hasDataRegStateChanged:" + hasDataRegStateChanged
                + ",hasChanged:" + hasChanged + ",hasVoiceRoamingOn:" + hasVoiceRoamingOn + ",hasVoiceRoamingOff:" + hasVoiceRoamingOff
                + ",hasDataRoamingOn:" + hasDataRoamingOn + ",hasDataRoamingOff:" + hasDataRoamingOff
                + ",hasLocationChanged:" + hasLocationChanged);
        // / @}

        // Add an event log when connection state changes
        if (mSS.getVoiceRegState() != mNewSS.getVoiceRegState()
                || mSS.getDataRegState() != mNewSS.getDataRegState()) {
            EventLog.writeEvent(EventLogTags.CDMA_SERVICE_STATE_CHANGE,
                    mSS.getVoiceRegState(), mSS.getDataRegState(),
                    mNewSS.getVoiceRegState(), mNewSS.getDataRegState());
        }

        // / M: c2k modify. @{
        if (mNewSS.getState() == ServiceState.STATE_IN_SERVICE) {
            mInService = true;
        } else {
            mInService = false;
        }
        log("pollStateDone: mInService = " + mInService);
        // / @}

        ServiceState tss;
        tss = mSS;
        mSS = mNewSS;
        mNewSS = tss;
        // clean slate for next time
        mNewSS.setStateOutOfService();

        CdmaCellLocation tcl = mCellLoc;
        mCellLoc = mNewCellLoc;
        mNewCellLoc = tcl;

        if (hasRilVoiceRadioTechnologyChanged) {
            updatePhoneObject();
        }

        if (hasRilDataRadioTechnologyChanged) {
            // / M: c2k modify. @{
            // query network time if Network Type is changed to a valid state
            if (mNewNetworkType != 0) {
                queryCurrentNitzTime();
            }
            // / @}
            mPhone.setSystemProperty(
                    TelephonyProperties.PROPERTY_DATA_NETWORK_TYPE,
                    ServiceState.rilRadioTechnologyToString(mSS
                            .getRilDataRadioTechnology()));
        }

        if (hasRegistered) {
            mNetworkAttachedRegistrants.notifyRegistrants();
        }

        TelephonyManager tm =
                (TelephonyManager) mPhone.getContext().getSystemService(Context.TELEPHONY_SERVICE);
        if (hasChanged) {
            if ((mCi.getRadioState().isOn()) && (!mIsSubscriptionFromRuim)) {
                log("pollStateDone isSubscriptionFromRuim = "
                        + mIsSubscriptionFromRuim);
                String eriText;
                // Now the CDMAPhone sees the new ServiceState so it can get the
                // new ERI text
                if (mSS.getVoiceRegState() == ServiceState.STATE_IN_SERVICE) {
                    eriText = mPhone.getCdmaEriText();
                } else {
                    // Note that ServiceState.STATE_OUT_OF_SERVICE is valid used
                    // for
                    // mRegistrationState 0,2,3 and 4
                    eriText = mPhone
                            .getContext()
                            .getText(
                                    com.android.internal.R.string.roamingTextSearching)
                            .toString();
                }
                mSS.setOperatorAlphaLong(eriText);
            }

            String operatorNumeric;

            mPhone.setSystemProperty(
                    TelephonyProperties.PROPERTY_OPERATOR_ALPHA,
                    mSS.getOperatorAlphaLong());

            String prevOperatorNumeric = SystemProperties.get(
                    TelephonyProperties.PROPERTY_OPERATOR_NUMERIC, "");
            operatorNumeric = mSS.getOperatorNumeric();

            // try to fix the invalid Operator Numeric
            if (isInvalidOperatorNumeric(operatorNumeric)) {
                int sid = mSS.getSystemId();
                operatorNumeric = fixUnknownMcc(operatorNumeric, sid);
            }

            tm.setNetworkOperatorNumericForPhone(mPhone.getPhoneId(), operatorNumeric);

            updateCarrierMccMncConfiguration(operatorNumeric,
                    prevOperatorNumeric, mPhone.getContext());

            if (isInvalidOperatorNumeric(operatorNumeric)) {
                if (DBG) {
                    log("operatorNumeric " + operatorNumeric + "is invalid");
                }
                tm.setNetworkCountryIsoForPhone(mPhone.getPhoneId(), "");
                mGotCountryCode = false;
            } else {
                String isoCountryCode = "";
                String mcc = operatorNumeric.substring(0, 3);
                try {
                    isoCountryCode = MccTable.countryCodeForMcc(Integer
                            .parseInt(operatorNumeric.substring(0, 3)));
                } catch (NumberFormatException ex) {
                    loge("pollStateDone: countryCodeForMcc error" + ex);
                } catch (StringIndexOutOfBoundsException ex) {
                    loge("pollStateDone: countryCodeForMcc error" + ex);
                }

                tm.setNetworkCountryIsoForPhone(mPhone.getPhoneId(), isoCountryCode);
                mGotCountryCode = true;

                setOperatorIdd(operatorNumeric);

                if (shouldFixTimeZoneNow(mPhone, operatorNumeric,
                        prevOperatorNumeric, mNeedFixZone)) {
                    fixTimeZone(isoCountryCode);
                }
            }

            mPhone.setSystemProperty(
                    TelephonyProperties.PROPERTY_OPERATOR_ISROAMING,
                    mSS.getRoaming() ? "true" : "false");

            //updateSpnDisplay();  //[ALPS02054692]

            // set roaming type
            setRoamingType(mSS);
            log("set roaming type");

            // / M: c2k modify. @{
            if (hasRegStateChanged) {
                if (mSS.getRegState() == ServiceState.REGISTRATION_STATE_UNKNOWN
                        && (1 == Settings.System.getInt(mPhone.getContext()
                                .getContentResolver(),
                                Settings.System.AIRPLANE_MODE_ON, -1))) {
                    int serviceState = mPhone.getServiceState().getState();
                    if (serviceState != ServiceState.STATE_POWER_OFF) {
                        mSS.setStateOff();
                    }
                }
            }
            // / @}
            mCDMASS = new ServiceState(mSS);
            updateSpnDisplay();  //[ALPS02054692]
            mPhone.notifyServiceStateChangedPForRegistrants(mCDMASS);
            combineGsmCdmaServiceState();
            mPhone.notifyServiceStateChangedForSvlte(mCombinedSS);
        }

        if (hasCdmaDataConnectionAttached) {
            mAttachedRegistrants.notifyRegistrants();
        }

        if (hasCdmaDataConnectionDetached) {
            mDetachedRegistrants.notifyRegistrants();
        }

        if (hasCdmaDataConnectionChanged || hasRilDataRadioTechnologyChanged) {
            notifyDataRegStateRilRadioTechnologyChanged();
            // M: [C2K][IRAT] Doesn't notify data connection for AP IRAT, since
            // the current PS may not on the side.
            if (!SvlteUtils.isActiveSvlteMode(mPhone)) {
                mPhone.notifyDataConnection(null);
            } else {
                if (SvlteUtils.getSvltePhoneProxy(mPhone.getPhoneId()).getPsPhone() == mPhone) {
                    mPhone.notifyDataConnection(null);
                } else {
                    log("Do nothing because it is not current PS phone");
                }
            }
        }

        if (hasVoiceRoamingOn) {
            mVoiceRoamingOnRegistrants.notifyRegistrants();
        }

        if (hasVoiceRoamingOff) {
            mVoiceRoamingOffRegistrants.notifyRegistrants();
        }

        if (hasDataRoamingOn) {
            mDataRoamingOnRegistrants.notifyRegistrants();
        }

        if (hasDataRoamingOff) {
            mDataRoamingOffRegistrants.notifyRegistrants();
        }

        if (hasLocationChanged) {
            mPhone.notifyLocationChanged();
        }
        // TODO: Add CdmaCellIdenity updating, see CdmaLteServiceStateTracker.
    }
/**
 * Notify the Service State Changed for SVLTE.
 * @param ss The Service State will be notified.
 */
    public void notifyServiceStateChanged(ServiceState ss) {
        mGSMSS = new ServiceState(ss);
        combineGsmCdmaServiceState();
        mPhone.notifyServiceStateChangedForSvlte(mCombinedSS);
    }

    public int getPhoneId() {
        return mPhone.getPhoneId();
    }

    @Override
    public int getUpdateSvlteSpnPhoneId(int sstType) {
        int updateSvlteSpnPhoneId = 0;
        mCDMASS = new ServiceState(mSS);
        if (mCDMASS.getVoiceRegState() == ServiceState.STATE_POWER_OFF
                && mCDMASS.getDataRegState() == ServiceState.STATE_POWER_OFF
                && mGSMSS.getVoiceRegState() == ServiceState.STATE_POWER_OFF
                && mGSMSS.getDataRegState() == ServiceState.STATE_POWER_OFF) {
            log("getUpdateSvlteSpnPhoneId, both phone power off");
            updateSvlteSpnPhoneId = (sstType == SST_TYPE) ? mPhone.getPhoneId() : SvlteUtils.
                    getLteDcPhoneId(SvlteUtils.getSlotId(mPhone.getPhoneId()));
        } else if (isSimLockedOrAbsent()) {
            log("getUpdateSvlteSpnPhoneId, CDMA card is absent or locked");
            updateSvlteSpnPhoneId = mPhone.getPhoneId();
        } else if (mCDMASS.getVoiceRegState() != ServiceState.STATE_IN_SERVICE
                && mCDMASS.getDataRegState() != ServiceState.STATE_IN_SERVICE) {
            log("getUpdateSvlteSpnPhoneId, cdma not in service");
            updateSvlteSpnPhoneId = SvlteUtils.
                    getLteDcPhoneId(SvlteUtils.getSlotId(mPhone.getPhoneId()));
        } else {
            log("getUpdateSvlteSpnPhoneId, other case");
            updateSvlteSpnPhoneId = mPhone.getPhoneId();
        }
        if (DBG) {
            log("getUpdateSvlteSpnPhoneId, phoneId =" + updateSvlteSpnPhoneId);
        }
        return updateSvlteSpnPhoneId;
    }

    /**
     * Update mGSMSS or mCDMASS to neweset state.
     * @param ss Newest service state used to update mGSMSS or mCDMASS
     * @param ssType 0 for mGSMSS, others for mCDMASS
     */
    public void updateGsmCdmaServiceState(ServiceState ss, int ssType) {
        if (ssType == 0) {
            mGSMSS = new ServiceState(ss);
            log("update mGSMSS = " + mGSMSS);
        } else {
            mCDMASS = new ServiceState(ss);
            log("update mCDMASS = " + mCDMASS);
        }
    }

    private void combineGsmCdmaServiceState() {
        if (DBG) {
            log("combineGsmCdmaServiceState, mCDMASS = " + mCDMASS
                    + " mGSMSS = " + mGSMSS);
        }
        //mCombinedSS = new ServiceState(mSS);
        ///Fix the emergency only not update issue.
        mCDMASS.setEmergencyOnly(mEmergencyOnly);
        if (mGSMSS.getVoiceRegState() == ServiceState.STATE_IN_SERVICE) {
            mCombinedSS = new ServiceState(mGSMSS);
        } else {
            mCombinedSS = new ServiceState(mCDMASS);
        }
        if (mGSMSS.getDataRegState() == ServiceState.STATE_IN_SERVICE) {
            mCombinedSS.setDataRegState(mGSMSS.getDataRegState());
            mCombinedSS.setRilDataRadioTechnology(mGSMSS
                    .getRilDataRadioTechnology());
            mCombinedSS.setRilDataRegState(mGSMSS.getRilDataRegState());
            mCombinedSS.setDataOperatorName(mGSMSS.getDataOperatorAlphaLong(),
                    mGSMSS.getDataOperatorAlphaShort(), mGSMSS.getDataOperatorNumeric());
        } else {
            mCombinedSS.setDataRegState(mCDMASS.getDataRegState());
            mCombinedSS.setRilDataRadioTechnology(mCDMASS
                    .getRilDataRadioTechnology());
            mCombinedSS.setRilDataRegState(mCDMASS.getRilDataRegState());
            mCombinedSS.setDataOperatorName(mCDMASS.getDataOperatorAlphaLong(),
                    mCDMASS.getDataOperatorAlphaShort(), mCDMASS.getDataOperatorNumeric());
        }
        mPhone.notifySvlteServiceStateChangedPForRegistrants(mCombinedSS);
        if (DBG) {
            log("combineGsmCdmaServiceState, mCombinedSS = " + mCombinedSS);
        }
    }

    /**
     * get the Service State for SVLTE.
     *
     * @return ServiceState the Service State for SVLTE.
     */
    @Override
    public ServiceState getSvlteServiceState() {
        return mCombinedSS;
    }

    @Override
    protected void log(String s) {
        Rlog.d(LOG_TAG, "[LteSST" + SvlteUtils.getSlotId(mPhone.getPhoneId())
                + "] " + s);
    }

    @Override
    protected void loge(String s) {
        Rlog.e(LOG_TAG, "[LteSST" + SvlteUtils.getSlotId(mPhone.getPhoneId())
                + "] " + s);
    }
}
