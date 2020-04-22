# SDIS FILE BACKUP DISTRIBUTED SYSTEM

# Compile

`sh compile.sh`

After compilation folders bin and res are created as well as files state file, chunk states file and deleted files file to keep track of peer state.

# Running Peer

`sh run.sh <protocol_version> <peer_id> <peer_access_point>`


# Running Test Client

### Backup a file

 `java backup.Test <host/accesspoint> BACKUP <file_path> <replication_degree>`

### Restore a file

`java backup.Test <host/accesspoint> RESTORE <file_name>`

### Delete a file

`java backup.Test <host/accesspoint> DELETE <file_name>`

### Reclaim space

`java backup.Test <host/accesspoint> RECLAIM <newSpace (KB)>`

### State

`java backup.Test <host/accesspoint> STATE`
