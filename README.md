# SDIS FILE BACKUP DISTRIBUTED SYSTEM

# Running Peer

* In the main folder execute sh run.sh <protocol_version> <peer_id> <peer_access_point>


# Running Test Client

* In the bin folder *

### Backup a file

 **run java backup.Test <host/accesspoint> BACKUP <file_path> <replication_degree>**

### Restore a file

 **run java backup.Test <host/accesspoint> RESTORE <file_name>**

### Delete a file

 **run java backup.Test <host/accesspoint> DELETE <file_name>**

### Reclaim space

 **run java backup.Test <host/accesspoint> RECLAIM <newSpace (KB)>**

### State

 **run java backup.Test <host/accesspoint> STATE**
