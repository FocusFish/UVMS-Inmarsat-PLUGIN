/*
﻿Developed with the contribution of the European Commission - Directorate General for Maritime Affairs and Fisheries
© European Union, 2015-2016.

This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can
redistribute it and/or modify it under the terms of the GNU General Public License as published by the
Free Software Foundation, either version 3 of the License, or any later version. The IFDM Suite is distributed in
the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details. You should have received a
copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.
 */
package fish.focus.uvms.plugins.inmarsat.data;

import fish.focus.schema.exchange.plugin.types.v1.PollType;

import java.time.Instant;

public class InmarsatPendingResponse {

    private PollType poll;
    private String msgId;
    private String unsentMsgId;
    private int referenceNumber;
    private StatusEnum status;
    private String mobTermId;
    private String dnId;
    private String membId;
    private Instant createdAt;

    public String getMobTermId() {
        return mobTermId;
    }

    public void setMobTermId(String mobTermId) {
        this.mobTermId = mobTermId;
    }

    public String getDnId() {
        return dnId;
    }

    public void setDnId(String dnId) {
        this.dnId = dnId;
    }

    public String getMembId() {
        return membId;
    }

    public void setMembId(String membId) {
        this.membId = membId;
    }

    public PollType getPollType() {
        return poll;
    }

    public void setPollType(PollType poll) {
        this.poll = poll;
    }

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public int getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(int referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public StatusEnum getStatus() {
        return status;
    }

    public void setStatus(StatusEnum status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InmarsatPendingResponse that = (InmarsatPendingResponse) o;
        return referenceNumber == that.referenceNumber;
    }

    @Override
    public int hashCode() {
        return referenceNumber;
    }

    public String getUnsentMsgId() {
        return unsentMsgId;
    }

    public void setUnsentMsgId(String unsentMsgId) {
        this.unsentMsgId = unsentMsgId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "InmarsatPendingResponse{" +
                "poll=" + poll +
                ", msgId='" + msgId + '\'' +
                ", referenceNumber=" + referenceNumber +
                ", status=" + status +
                ", mobTermId='" + mobTermId + '\'' +
                ", dnId='" + dnId + '\'' +
                ", membId='" + membId + '\'' +
                '}';
    }
}
