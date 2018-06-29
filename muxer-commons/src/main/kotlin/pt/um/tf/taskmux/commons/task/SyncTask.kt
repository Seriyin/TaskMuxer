package pt.um.tf.taskmux.commons.task;

import pt.um.tf.taskmux.commons.URIGenerator

abstract class SyncTask<T>(u : URIGenerator = URIGenerator()) : Task<Result<T>>(u)
