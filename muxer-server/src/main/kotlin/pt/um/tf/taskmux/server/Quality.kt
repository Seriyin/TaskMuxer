package pt.um.tf.taskmux.server;

/**
 * Describes typical pt.um.tf.server flow.
 * If we're solo we must be the leader.
 * This is a very strong assumption but necessary in this simplified model.
 *
 * Election is automatic and elects smallest id.
 *
 * We find others in the group as followers to be notified.
 */
enum class Quality {
    LEADER {
        override fun follow() = LEADER
        override fun rise() = LEADER
    },
    FOLLOWER {
        override fun follow() = FOLLOWER
        override fun rise() = LEADER
    },
    NOT_READY {
        override fun follow() = FOLLOWER
        override fun rise() = NOT_READY
    };

    abstract fun rise() : Quality
    abstract fun follow() : Quality

    companion object {
        fun initNotReady() : Quality {
            return Quality.NOT_READY
        }

        fun first() : Quality {
            return Quality.LEADER
        }
    }

}
