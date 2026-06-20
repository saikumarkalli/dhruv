Use the `dhruv-data-engineer` agent to build the Room/persistence data layer.

Arguments: $ARGUMENTS

Hand off to dhruv-data-engineer with this task: $ARGUMENTS. Follow the dhruv-room-entity skill. Create the Room entity implementing DhruvEntity (id, userId, createdAt, updatedAt, isSynced, isDeleted), write the DAO, repository, and any necessary migrations. Add userId index from day one. Wire into the shared :apps:<app>:data module — do not create a separate database per feature.
