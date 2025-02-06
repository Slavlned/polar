#                 #
# Пример          #
# Камень, ножницы #
# бумага 🪨📜✂️  #
use 'lib.random'

# Доступные варианты #
# 1 - камень #
# 2 - ножницы #
# 3 - бумага #
possible = [1, 2, 3]
score = 0
bot_score = 0

# Инпут пользователя #
func user_input() {
    put('Выберите вариант:')
    put('1 - камень 🪨. 2 - ножницы ✂️.')
    put('3 - бумага 📜.')
    safe {
        val = number(scan('Выбор: '))
    } handle(e) {
        put('Неправильный ввод!')
        back(user_input())
    }
    if (val > 3 or val < 1) {
        put('Неправильный ввод!')
    }
    back(val)
}

# цикл #
while (true) {
    # выбор бота и пользователя #
    chosen = Random.choice(possible)
    input = user_input()
    # проверяем #
    if (input == chosen) {
        put('Ничья!')
    }
    # если игрок выбрал камень #
    elif (input == 1) {
        if (chosen == 2) {
            put('Победа! Бот выбрал ножницы ✂️')
            score += 1
        }
        elif (chosen == 3) {
            put('Проигыршь! Бот выбрал бумагу 📜')
            bot_score += 1
        }
    }
    # если игрок выбрал ножницы #
    elif (input == 2) {
        if (chosen == 1) {
            put('Проигрышь! Бот выбрал камень 🪨')
            bot_score += 1
        }
        elif (chosen == 3) {
            put('Победа! Бот выбрал бумагу 📜')
            score += 1
        }
    }
    # если игрок выбрал бумагу #
    elif (input == 3) {
        if (chosen == 1) {
            put('Победа! Бот выбрал камень 🪨')
            score += 1
        }
        elif (chosen == 2) {
            put('Проигрышь! Бот выбрал ножницы 📜')
            bot_score += 1
        }
    }
    # выводим счёт #
    put('#################')
    put('##     СЧЁТ    ')
    put('## ИГРОК 🥬: ' + string(score))
    put('## БОТ 🤖: ' + string(bot_score))
    put('#################')
}