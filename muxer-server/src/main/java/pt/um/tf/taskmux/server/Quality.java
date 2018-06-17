package pt.um.tf.taskmux.server;

/**
 * Describes typical server flow.
 * If we're solo we must be the leader.
 * This is a very strong assumption but necessary in this simplified model.
 *
 * Election is automatic and elects smallest id.
 *
 * We find others in the group as followers to be notified.
 */
public enum Quality {
    LEADER {
        public Quality follow() {
            return LEADER;
        }
        public Quality rise() {
            return LEADER;
        }
    },
    FOLLOWER {
        public Quality follow() {
            return FOLLOWER;
        }
        public Quality rise(){
            return LEADER;
        }
    },
    NOT_READY {
        public Quality follow() {
            return FOLLOWER;
        }

        public Quality rise() {
            return NOT_READY;
        }
    };

    public abstract Quality rise();
    public abstract Quality follow();

    public static Quality initNotReady() {
        return Quality.NOT_READY;
    }

    public static Quality first() {
        return Quality.LEADER;
    }
}
