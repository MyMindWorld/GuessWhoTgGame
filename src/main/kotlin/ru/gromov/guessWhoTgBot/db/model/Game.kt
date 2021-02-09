package ru.gromov.guessWhoTgBot.db.model

import lombok.AllArgsConstructor
import lombok.Data
import lombok.EqualsAndHashCode
import lombok.NoArgsConstructor
import java.util.*
import java.util.stream.Collectors
import javax.persistence.*

@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "games")
@Entity(name = "game")
class Game(
    @Id
    @GeneratedValue
    var id: Long? = null,

    @OneToMany(
        mappedBy = "currentGame",
        fetch = FetchType.EAGER
    )
    @EqualsAndHashCode.Exclude
    var users: List<User> = arrayListOf(),

    @Column(name = "joinCode")
    var joinCode: String = UUID.randomUUID().toString(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator", referencedColumnName = "id", insertable = true, updatable = true)
    @EqualsAndHashCode.Exclude
    var creator: User = User(),

    @Column(name = "isFinished")
    var isFinished: Boolean = false,

    @Column(name = "isStarted")
    var isStarted: Boolean = false
) {

    @Override
    override fun toString(): String {
        return "Game with id $id and joinCode $joinCode with users total of ${users.size}. Finished - $isFinished"
    }

    fun isReadyToStart(): Boolean {
        return users.stream().allMatch { user -> user.riddledPerson != "" }
    }

    fun notReadyUsers(): List<User> {
        return users.stream().filter { user -> user.makesRiddleFor!!.riddledPerson == "" }
            .collect(Collectors.toList())
    }
}
