package ru.gromov.guessWhoTgBot.db.model

import lombok.*
import javax.persistence.*

@Data
@EqualsAndHashCode
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
@Entity(name = "user")
class User(
    @Id
    var id: Long? = null,
    @Column(name = "name")
    var name: String? = null,
    @Column(name = "chatId")
    var chatId: Long? = null,
    @Column(name = "username")
    var username: String? = null,

    @ManyToOne(fetch = FetchType.EAGER)
    @EqualsAndHashCode.Exclude
    var currentGame: Game? = null,

    @OneToOne(optional = true)
    @JoinColumn(name = "makesRiddleFor", referencedColumnName = "ID")
    @EqualsAndHashCode.Exclude
    var makesRiddleFor: User? = null,

    @Column(name = "riddledPerson")
    var riddledPerson: String = ""
) {
    @Override
    override fun toString(): String {
        return "User with id $id and name $name in chat $chatId. Current game - ${currentGame?.id ?: "not set"}"
    }

    fun getMentionLink(): String {
        return "[$name](tg://user?id=$chatId)"
    }

    fun getRiddledPersonGoogleLink(): String {
        return "[$riddledPerson](https://www.google.com/search?q=${
            riddledPerson.split(" ").joinToString(separator = "+")
        })"
    }
}


