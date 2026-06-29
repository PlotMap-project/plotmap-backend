package com.plotmap.backend.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.io.Serializable
import java.util.UUID

class UserToProjectId(
    val idUser: UUID = UUID(0, 0),
    val idProject: UUID = UUID(0, 0)
) : Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UserToProjectId) return false
        return idUser == other.idUser && idProject == other.idProject
    }

    override fun hashCode(): Int {
        return 31 * idUser.hashCode() + idProject.hashCode()
    }
}

@Entity
@Table(name = "user_to_projects")
@IdClass(UserToProjectId::class)
class UserToProject(
    @Id
    @Column(name = "id_user")
    val idUser: UUID,

    @Id
    @Column(name = "id_project")
    val idProject: UUID
)
