package de.carsten.android.muzzic.id3.model.frame.binary

/**
 * Single involved-person entry of the deprecated IPLS frame.
 *
 * @property involvement involvement description, e.g. "producer".
 * @property name name of the involved person.
 */
data class InvolvedPerson(val involvement: String, val name: String)
