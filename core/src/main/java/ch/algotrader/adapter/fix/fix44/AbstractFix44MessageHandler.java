/***********************************************************************************
 * AlgoTrader Enterprise Trading Framework
 *
 * Copyright (C) 2013 Flury Trading - All rights reserved
 *
 * All information contained herein is, and remains the property of Flury Trading.
 * The intellectual and technical concepts contained herein are proprietary to
 * Flury Trading. Modification, translation, reverse engineering, decompilation,
 * disassembly or reproduction of this material is strictly forbidden unless prior
 * written permission is obtained from Flury Trading
 *
 * Fur detailed terms and conditions consult the file LICENSE.txt or contact
 *
 * Flury Trading
 * Badenerstrasse 16
 * 8004 Zurich
 ***********************************************************************************/
package ch.algotrader.adapter.fix.fix44;

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
import quickfix.fix44.BusinessMessageReject;
import quickfix.fix44.Reject;

/**
 * Base Fix4.4 message handler that handles {@link Reject} and {@link BusinessMessageReject} rejection messages.
 *
 * @author <a href="mailto:okalnichevski@algotrader.ch">Oleg Kalnichevski</a>
 *
 * @version $Revision: 6665 $ $Date: 2014-01-11 18:17:51 +0100 (Sa, 11 Jan 2014) $
 */
public abstract class AbstractFix44MessageHandler {

    private static Logger LOGGER = MyLogger.getLogger(AbstractFix44MessageHandler.class.getName());

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
