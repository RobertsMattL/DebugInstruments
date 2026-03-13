package com.debuginstruments.app.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ContactRepository(private val contactDao: ContactDao) {

    fun getAllContacts(): Flow<List<Contact>> = contactDao.getAllContacts()

    fun getContactById(id: Long): Flow<Contact?> = contactDao.getContactById(id)

    fun insertContact(contact: Contact): Flow<Long> = flow {
        emit(contactDao.insert(contact))
    }

    fun updateContact(contact: Contact): Flow<Unit> = flow {
        emit(contactDao.update(contact))
    }

    fun deleteContact(contact: Contact): Flow<Unit> = flow {
        emit(contactDao.delete(contact))
    }
}
