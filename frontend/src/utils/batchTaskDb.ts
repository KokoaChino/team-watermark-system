import { openDB, type DBSchema, type IDBPDatabase } from 'idb'
import type { BatchTaskExecutionSession } from '@/types'
import { cloneExecutionSession } from '@/utils/batchTaskExecution'

type BinaryStoreName = 'source-files' | 'watermark-files' | 'result-files' | 'artifacts'

interface BatchTaskBinaryRecord {
  key: string
  sessionId: string
  name: string
  type: string
  blob: Blob
  itemId?: string
  watermarkId?: string
  updatedAt: string
}

interface BatchTaskExecutionDB extends DBSchema {
  sessions: {
    key: string
    value: BatchTaskExecutionSession
  }
  'source-files': {
    key: string
    value: BatchTaskBinaryRecord
    indexes: {
      'by-session': string
    }
  }
  'watermark-files': {
    key: string
    value: BatchTaskBinaryRecord
    indexes: {
      'by-session': string
    }
  }
  'result-files': {
    key: string
    value: BatchTaskBinaryRecord
    indexes: {
      'by-session': string
    }
  }
  artifacts: {
    key: string
    value: BatchTaskBinaryRecord
    indexes: {
      'by-session': string
    }
  }
}

const DB_NAME = 'team-watermark-batch-task'
const DB_VERSION = 1

let dbPromise: Promise<IDBPDatabase<BatchTaskExecutionDB>> | null = null

function getDb() {
  if (!dbPromise) {
    dbPromise = openDB<BatchTaskExecutionDB>(DB_NAME, DB_VERSION, {
      upgrade(db) {
        if (!db.objectStoreNames.contains('sessions')) {
          db.createObjectStore('sessions', { keyPath: 'id' })
        }

        createBinaryStore(db, 'source-files')
        createBinaryStore(db, 'watermark-files')
        createBinaryStore(db, 'result-files')
        createBinaryStore(db, 'artifacts')
      }
    })
  }

  return dbPromise
}

function createBinaryStore(db: IDBPDatabase<BatchTaskExecutionDB>, storeName: BinaryStoreName) {
  if (db.objectStoreNames.contains(storeName)) {
    return
  }

  const store = db.createObjectStore(storeName, { keyPath: 'key' })
  store.createIndex('by-session', 'sessionId', { unique: false })
}

function buildBinaryRecord(options: {
  key: string
  sessionId: string
  name: string
  type: string
  blob: Blob
  itemId?: string
  watermarkId?: string
}): BatchTaskBinaryRecord {
  return {
    ...options,
    updatedAt: new Date().toISOString()
  }
}

async function putBinaryRecord(storeName: BinaryStoreName, record: BatchTaskBinaryRecord) {
  const db = await getDb()
  await db.put(storeName, record)
}

async function getBinaryRecord(storeName: BinaryStoreName, key: string) {
  const db = await getDb()
  return db.get(storeName, key)
}

async function deleteBinaryRecord(storeName: BinaryStoreName, key: string) {
  const db = await getDb()
  await db.delete(storeName, key)
}

async function deleteSessionStoreRecords(storeName: BinaryStoreName, sessionId: string) {
  const db = await getDb()
  const transaction = db.transaction(storeName, 'readwrite')
  const store = transaction.objectStore(storeName)
  const keys = await store.index('by-session').getAllKeys(sessionId)

  await Promise.all(keys.map((key) => store.delete(key)))
  await transaction.done
}

export async function putExecutionSession(session: BatchTaskExecutionSession) {
  const db = await getDb()
  await db.put('sessions', cloneExecutionSession(session))
}

export async function getExecutionSession(sessionId: string) {
  const db = await getDb()
  return db.get('sessions', sessionId)
}

export async function deleteExecutionSession(sessionId: string) {
  const db = await getDb()
  await db.delete('sessions', sessionId)
}

export async function putSourceFile(options: {
  key: string
  sessionId: string
  itemId: string
  file: File
}) {
  await putBinaryRecord(
    'source-files',
    buildBinaryRecord({
      key: options.key,
      sessionId: options.sessionId,
      itemId: options.itemId,
      name: options.file.name,
      type: options.file.type,
      blob: options.file
    })
  )
}

export async function getSourceFile(key: string) {
  const record = await getBinaryRecord('source-files', key)
  if (!record) {
    return null
  }

  return new File([record.blob], record.name, {
    type: record.type,
    lastModified: Date.now()
  })
}

export async function putWatermarkFile(options: {
  key: string
  sessionId: string
  itemId: string
  watermarkId: string
  file: File
}) {
  await putBinaryRecord(
    'watermark-files',
    buildBinaryRecord({
      key: options.key,
      sessionId: options.sessionId,
      itemId: options.itemId,
      watermarkId: options.watermarkId,
      name: options.file.name,
      type: options.file.type,
      blob: options.file
    })
  )
}

export async function getWatermarkFile(key: string) {
  const record = await getBinaryRecord('watermark-files', key)
  if (!record) {
    return null
  }

  return new File([record.blob], record.name, {
    type: record.type,
    lastModified: Date.now()
  })
}

export async function putResultFile(options: {
  key: string
  sessionId: string
  itemId: string
  fileName: string
  mimeType: string
  blob: Blob
}) {
  await putBinaryRecord(
    'result-files',
    buildBinaryRecord({
      key: options.key,
      sessionId: options.sessionId,
      itemId: options.itemId,
      name: options.fileName,
      type: options.mimeType,
      blob: options.blob
    })
  )
}

export async function getResultFile(key: string) {
  const record = await getBinaryRecord('result-files', key)
  if (!record) {
    return null
  }

  return new File([record.blob], record.name, {
    type: record.type,
    lastModified: Date.now()
  })
}

export async function deleteResultFile(key: string) {
  await deleteBinaryRecord('result-files', key)
}

export async function putArtifact(options: {
  key: string
  sessionId: string
  fileName: string
  mimeType: string
  blob: Blob
}) {
  await putBinaryRecord(
    'artifacts',
    buildBinaryRecord({
      key: options.key,
      sessionId: options.sessionId,
      name: options.fileName,
      type: options.mimeType,
      blob: options.blob
    })
  )
}

export async function getArtifact(key: string) {
  const record = await getBinaryRecord('artifacts', key)
  if (!record) {
    return null
  }

  return new File([record.blob], record.name, {
    type: record.type,
    lastModified: Date.now()
  })
}

export async function deleteArtifact(key: string) {
  await deleteBinaryRecord('artifacts', key)
}

export async function clearExecutionSessionData(sessionId: string) {
  await Promise.all([
    deleteExecutionSession(sessionId),
    deleteSessionStoreRecords('source-files', sessionId),
    deleteSessionStoreRecords('watermark-files', sessionId),
    deleteSessionStoreRecords('result-files', sessionId),
    deleteSessionStoreRecords('artifacts', sessionId)
  ])
}



