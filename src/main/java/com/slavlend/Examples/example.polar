# Библиотеки #
use 'lib.console'
use 'lib.array'
use 'lib.str'
use 'lib.ant'

# Основной шаблон кадра #
base_frame = [
     '...................................................................................................',
     '...................................................................................................',
     '...................................................................................................',
     '...................................................................................................',
     '...................................................................................................',
     '...................................................................................................',
     '.......................................................#...........................................',
     '...........%................................%#**+++===*%.....%+%...................................',
     '..........................................%##***++++===--:-#........................#+.............',
     '........................................%%##***++===--:::::::%......................%%.............',
     '.......................................%##**+++==-----:::::::.-++++++++++++**#%....................',
     '......................................%%##**+++===----:::::..::+.%%#**#**++++*=+++.................',
     '.....................................%%%##**+++===---::::::::...%......#****+=**+++%...............',
     '.....................................%%%##***++=====----:::.....*......%*#**+++#=++#...............',
     '.........................##..........%%%%##**+++===---:::::.:...+.....#*#**+=#+=++*................',
     '......................#++*=***%......%%%%#**+++====---::::::..::#..%***#++=%=+++*..................',
     '....................*+=#=+**#*##%......%%%%##**++===-------::--==-*#***+=+#==*++...................',
     '...................++=#+++*#*##%#**++++++++++++=----------::----+++++++*++=**......................',
     '..................%+++=%=++********###%#****++======+====---::+*=++++#.............................',
     '........#:%.......#++++=+#*+=+++++******++++=====------::::=++*%...................................',
     '......................%*++++++===++*****+++=------------=+%........................%...............',
     '.............................%+#%#########%%%...%%####%............................................',
     '...................................................................................................',
     '...................................................................................................',
     '...................................................................................................',
     '...................................................................................................',
     '...................................................................................................',
     '...................................................................................................',
     '...................................................................................................',
     '...................................................................................................'
]

# Создаем анимацию #
frames = []

# Поворот строчки #
func rotate_row(row, i) = {
    n = @len(row)
    result = []
    for (y = 0, y < n) {
        @result.add(' ')
        y += 1
    }
    for (j = 0, j < n) {
        new_index = (j + i) % n
        @result.set(new_index, @Str.at(row, j))
        j += 1
    }
    @back(@result.stringify())
}

# Процесс анимирования #
for (i = 0, i < 100) {
    @put('frame: ' + i)
    frame_rot = []
    each(row, base_frame) {
        @frame_rot.add(@rotate_row(row, i))
    }
    @frames.add(frame_rot)
    i += 1
}

# Вывод анимации в консоли #
while (1 == 1) {
    each(frame, frames) {
        for (i = 0, i < @frame.size()+1) {
            @put('')
            i += 1
        }
        each (row, frame) {
            @put(row)  # Печать текущего кадра #
        }
        @sleep(100)  # Задержка между кадрами #
    }
}