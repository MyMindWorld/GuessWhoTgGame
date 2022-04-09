package ru.gromov.guessWhoTgBot.db.model

import lombok.AllArgsConstructor
import lombok.Data
import lombok.EqualsAndHashCode
import lombok.NoArgsConstructor
import javax.persistence.*

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
class User(
    @Id
    val id: Long? = null,
    @Column(name = "name")
    val name: String = "",
    @Column(name = "chatId")
    val chatId: Long? = null,
    @Column(name = "username")
    val username: String = "",

    @ManyToOne(optional = true, cascade = [CascadeType.ALL],fetch = FetchType.EAGER)
//    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="game_id", nullable=true)
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


