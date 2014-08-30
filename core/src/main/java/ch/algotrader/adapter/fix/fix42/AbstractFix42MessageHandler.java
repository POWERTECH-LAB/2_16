/***********************************************************************************
 * AlgoTrader Enterprise Trading Framework
 *
 * Copyright (C) 2014 AlgoTrader GmbH - All rights reserved
 *
 * All information contained herein is, and remains the property of AlgoTrader GmbH.
 * The intellectual and technical concepts contained herein are proprietary to
 * AlgoTrader GmbH. Modification, translation, reverse engineering, decompilation,
 * disassembly or reproduction of this material is strictly forbidden unless prior
 * written permission is obtained from AlgoTrader GmbH
 *
 * Fur detailed terms and conditions consult the file LICENSE.txt or contact
 *
 * AlgoTrader GmbH
 * Badenerstrasse 16
 * 8004 Zurich
 ***********************************************************************************/
package ch.algotrader.adapter.fix.fix42;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import ch.algotrader.util.MyLogger;
import quickfix.FieldNotFound;
import quickfix.SessionID;
import quickfix.field.BusinessRejectReason;
import quickfix.field.BusinessRejectRefID;
import quickfix.field.RefMsgType;
import quickfix.field.RefSeqNum;
import quickfix.field.RefTagID;
import quickfix.field.SessionRejectReason;
import quickfix.field.Text;
import quickfix.fix42.BusinessMessageReject;
import quickfix.fix42.Reject;

/**
 * Base Fix/4.2 message handler that handles {@link quickfix.fix42.Reject} and {@link quickfix.fix42.BusinessMessageReject} rejection messages.
 *
 * @author <a href="mailto:okalnichevski@algotrader.ch">Oleg Kalnichevski</a>
 *
 * @version $Revision$ $Date$
 */
public abstract class AbstractFix42MessageHandler {

    private static Logger LOGGER = MyLogger.getLogger(AbstractFix42MessageHandler.class.getName());

    public void onMessage(final Reject reject, final SessionID sessionID) throws FieldNotFound {

        if (LOGGER.isEnabledFor(Level.ERROR)) {
            StringBuilder buf = new StringBuilder();
            buf.append("Message rejected as invalid");
            if (reject.isSetField(RefSeqNum.FIELD)) {
                int seqNum = reject.getRefSeqNum().getValue();
                buf.append(" [seq num: ").append(seqNum).append("]");
            }
            if (reject.isSetField(RefMsgType.FIELD) && reject.isSetField(RefTagID.FIELD)) {
                String msgType = reject.getRefMsgType().getValue();
                int tagId = reject.getRefTagID().getValue();
                buf.append(" [message type: ").append(msgType).append("; tag id: ").append(tagId).append("]");
            }
            if (reject.isSetField(Text.FIELD)) {
                buf.append(": ").append(reject.getString(Text.FIELD));
            } else if (reject.isSetField(SessionRejectReason.FIELD)) {
                SessionRejectReason reason = reject.getSessionRejectReason();
                buf.append(": reason code ").append(reason.getValue());
            }

            LOGGER.error(buf.toString());
        }
    }

    public void onMessage(BusinessMessageReject reject, SessionID sessionID) throws FieldNotFound {

        if (LOGGER.isEnabledFor(Level.ERROR)) {

            StringBuilder buf = new StringBuilder();
            buf.append("Message rejected as invalid (business reject)");
            if (reject.isSetField(RefSeqNum.FIELD)) {
                int seqNum = reject.getRefSeqNum().getValue();
                buf.append(" [seq num: ").append(seqNum).append("]");
            }
            if (reject.isSetField(RefMsgType.FIELD) && reject.isSetField(BusinessRejectRefID.FIELD)) {
                String msgType = reject.getRefMsgType().getValue();
                String id = reject.getBusinessRejectRefID().getValue();
                buf.append(" [message type: ").append(msgType).append("; message id: ").append(id).append("]");
            }
            if (reject.isSetField(Text.FIELD)) {
                buf.append(": ").append(reject.getString(Text.FIELD));
            } else if (reject.isSetField(BusinessRejectReason.FIELD)) {
                BusinessRejectReason reason = reject.getBusinessRejectReason();
                buf.append(": reason code ").append(reason.getValue());
            }

            LOGGER.error(buf.toString());
        }
    }

}
