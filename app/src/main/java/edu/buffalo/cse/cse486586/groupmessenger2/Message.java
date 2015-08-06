package edu.buffalo.cse.cse486586.groupmessenger2;

/**
 * Created by abhijit on 3/15/15.
 */
public class Message implements Comparable<Message> {
        String msg;
        double sequenceNumber;
        int messageId;
        int processId;
        boolean deliverable;

        public String getMsg() {
            return msg;
        }

        public void setMsg(String msg) {
            this.msg = msg;
        }

        public double getSequenceNumber() {
            return sequenceNumber;
        }

        public void setSequenceNumber(double sequenceNumber) {
            this.sequenceNumber = sequenceNumber;
        }

        public int getMessageId() {
            return messageId;
        }

        public void setMessageId(int messageId) {
            this.messageId = messageId;
        }

        public int getProcessId() {
            return processId;
        }

        public void setProcessId(int processId) {
            this.processId = processId;
        }

        public boolean isDeliverable() {
            return deliverable;
        }

        public void setDeliverable(boolean deliverable) {
            this.deliverable = deliverable;
        }

        public int compareTo(Message o) {
            if(this.sequenceNumber == o.sequenceNumber){
                return 0;
            }
            else{
                return this.sequenceNumber<o.sequenceNumber?-1:1;
            }
        }


}
