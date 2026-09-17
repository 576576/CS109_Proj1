# CS109 消消乐 —— Model 精简与现代化分步变更计划

> 目标：把 `src/model` 从"带 getter 的二维数组"改造成**真正承载规则的领域模型**，
> 同步消灭反向依赖、字符串类型化、重复类和隐式存档格式。
> 每一步都是独立可提交、可编译、可冒烟验证的原子变更。

---

## 0. 现状诊断（为什么需要改）

| # | 问题 | 位置 | 影响 |
|---|---|---|---|
| P1 | **层倒错**：model 反向依赖 view | `Chessboard.java:4` 静态导入 `view.ChessComponent.chessTypes` | 模型的数据源定义在 UI 类里，model 无法独立编译/测试 |
| P2 | **循环依赖**：model 写 controller 状态 | `Chessboard.java:3,30,48` 写 `controller.GameController.isNewGameInitialized`；`GameController` 又持有 `Chessboard` | model↔controller 双向环，初始化顺序一改就崩 |
| P3 | **空壳类** `Cell` | `Cell.java` 只包一个 `ChessPiece`，且 `implements Serializable` 但全项目无任何 Java 序列化 | 多一层无意义间接，`getGrid()[i][j].getPiece()` 到处套娃 |
| P4 | **字符串类型化** | 棋子身份是 emoji `String`，6 处 `.getName().equals(...)` 比身份 | 拼错不报错；`Constant.colorMap` 与 `view.ChessComponent.chessTypes` 是同一份知识的两处副本，必须手工同步 |
| P5 | **枚举滥用** | `Constant` 是enum 当常量柜，`DEFAULT_CHESSBOARD_ROW_SIZE.getNum()` 被调用 40+ 次 | 每次调用都过一遍方法，且尺寸被写死为常量无法配置 |
| P6 | **逻辑泄漏** | 全部核心规则（消除扫描、重力、补棋、空位检测、有无可行解、提示）都在 `GameController`，约 250 行 | model 是贫血模型；规则无法复用、无法单测 |
| P7 | **内部数组外泄** | `Chessboard.getGrid()` 返回裸 `Cell[][]` | 外部可直接改写棋盘且不触发任何一致性约束 |
| P8 | **异常控制流** | `checkerBoardValidator` 用 `try/catch (NullPointerException ignored)` 判空 + `static` 收裸二维数组 | 掩盖真实 bug；命名有误导（返回 true = 存在三连，不像"校验通过"） |
| P9 | **难度类重复** | `Difficulty` 构造函数里反向和 `DifficultyPreset` 比 `equals` 来决定名字；`GameController` 又抄了一份 `ArrayList<DifficultyPreset>`；load 时再做一遍"回匹配" | 三处维护同一份预设列表，极易不一致 |
| P10 | **存档格式隐式耦合 + 现成 bug** | `GameController` 535 行里塞了 4 段几乎重复的 IO/解析；`Math.clamp(idx, 0, chessTypes.length)` 上界越界（应为 `length-1`）；缺失值兜底 `nextInt(4)` 与 6 种棋子不符；写入 `[i][j]` 读取 `[j][i]` 行列转置不一致 | 档损坏、加载出非法棋子 |
| P11 | **硬编码尺寸** | `new int[8][8]`、`CHESS_SIZE * 8` 散落在 model/controller/view | 改棋盘尺寸要改 5 处 |
| P12 | **工具杂烩** | `Util.RandomPick`（违反 Java 方法命名规范、用 `Math.random()`）、`getFileExtension`（与 model 无关却躺在 model） | 职责不清 |
| P13 | **命名借壳** | 这是消消乐，类名却全叫 `Chessboard/ChessPiece/ChessComponent` | 读代码的人一直被误导 |

**顺带发现的现成 bug**（重构中会自然修掉，不单独列步）：
- `GameController.initialize():136` `if (isNotContinuable()) initialize();` 递归无出口，极端情况爆栈。
- `doAutoConfirm():812` / `doAutoMode()` 是**无 sleep 的忙循环线程**，CPU 打满且永不退出。
- `loadFromFile` 不重开计时器，`loadFromString` 会 —— 两条加载路径行为不一致。
- `hint()` 在搜索过程中真实修改棋盘的选中态并 swap 模型，是"带副作用的查询"。

---

## 目标架构

```
src/model/                        ← 只依赖 JDK，零上层 import
  PieceType.java        enum  棋子类目：颜色 / 贴图序号 / 字形 / random(rng)
  Piece.java            record 棋子（当前可退化到 PieceType，保留以应对后续道具/特效）
  BoardPoint.java       record 坐标 + distanceTo / neighbors
  Board.java            final 棋盘：规则唯一归属地
  BoardSnapshot.java    record 棋盘不可变快照（视图唯一入口）
  Move.java             record 重力位移(from, to)
  Spawn.java            record 顶部新生成(point, type)
  Swap.java             record 一次可行交换(a, b)
  Difficulty.java       record 难度数据 + hasStepLimit()/hasTimeLimit()
  DifficultyPreset.java enum implements DifficultySpec
  GameState.java        record 完整局状态
  GameStateCodec.java   存档文本 ↔ GameState（带版本号）
  PieceBag.java         可注入 RNG 的发牌器（保证可复现）
```

四条硬约束，全程用 grep 断言：

```bash
# ① model 不得引用上层（Step 2 之后必须为空）
grep -rn "^import \(static \)\?\(view\|controller\|net\|listener\|player\)\." src/model/
# ② 不得外泄内部数组（Step 7 之后必须为空）
grep -rn "getGrid()" src/
# ③ 不得再出现 enum 常量柜（Step 8 之后必须为空）
grep -rn "DEFAULT_CHESSBOARD\|Constant\." src/
# ④ 不得再以 String 比棋子身份（Step 2 之后必须为空）
grep -rn "getName()" src/
```

---

## Step 0 —— 地基：建立可复现的验证基线

**为什么先做这个**：项目现在没有任何测试基建（`lib/` 只有 jflac/mp3spi 等音效 jar，无 JUnit），
重构一旦改变了初始化顺序或随机调用次数，行为会静默漂移。先造一把尺子。

**动作**
1. `git tag refactor-baseline` 打基线。
2. 新增 `smoke/SmokeTest.java`（**纯 main 方法，零外部依赖**，便于用 `java` 直接跑）：
   - `initNoMatch`：初始化 200 次，断言开局不存在三连；
   - `gravityNoHole`：随机执行 500 次「消除→重力→补棋」，断言结束时无空格；
   - `saveLoadRoundTrip`：随机局面 `toText → fromText`，断言完全相等；
   - `hintSoundness`：`findHint()` 返回的 swap 执行后必须真的产生三连。
3. **把随机源显式化**：新增 `PieceBag`，内部持有 `java.util.random.RandomGenerator`，
   构造时接受固定种子。所有 `Util.RandomPick` / `new Random()` 调用改为走它。
   → 这一步让上面 4 个断言变得**可复现**，是后面 11 步能安全推进的前提。

**验收**：`javac -d out $(find src -name '*.java')` 通过；`java -cp out smoke.SmokeTest` 全绿。
**风险**：无（纯新增）。

---

## Step 1 —— 机械改名（推荐现在就做，也可最后做）

纯 IDE 重构，语义零变化，但**早做能让后续每一步的 diff 都干净**，强烈建议先执行。

| 现名 | 新名 |
|---|---|
| `model.Chessboard` | `model.Board` |
| `model.ChessPiece` | `model.Piece` |
| `model.ChessboardPoint` | `model.BoardPoint` |
| `view.ChessboardComponent` | `view.BoardView` |
| `view.ChessComponent` | `view.TileView` |
| `view.ChessGameFrame` | `view.GameFrame` |

注意：`resource/texture/chess/` 与 `select_pointer.png` 是**磁盘路径字符串**，不参与重命名；
是否一并改成 `resource/texture/tile/` 可自行决定 —— 与 `.gitignore` 无关，但要同步 `TileView` 里的路径拼接。

**验收**：全项目编译通过 + Step 0 冒烟全绿 + `grep -rn "Chess" src/ | wc -l` 显著下降。
**风险**：低。唯一坑是 `ImageUtils` / `MusicPlayer` 里的字符串常量，跟类名无关但容易误替，用 IDE 的"重构→重命名"而非全局正则。

---

## Step 2 —— 引入 `PieceType` 枚举，消灭字符串类型化 + 切断 model→view（核心）

这是**收益最大**的一步，把 P1/P4 一起解决。

**动作**
1. 新建 `model/PieceType.java`：把 `view.ChessComponent.chessTypes` 的真相整体下移。

```java
public enum PieceType {
    DIAMOND(0, Color.BLUE,    "\uD83D\uDC8E"),   // 💎
    ORB    (1, Color.WHITE,   "⚪"),
    TRIANGLE(2, Color.GREEN,  "▲"),
    HEXAGON(3, Color.ORANGE,  "\uD83D\uDD36"),   // 🔶
    SMILE  (4, Color.YELLOW,  "\uD83D\uDE42"),   // 🙂
    EYE    (5, Color.MAGENTA, "\uD83D\uDC40");   // 👀

    private final int textureIndex;
    private final Color color;
    private final String glyph;

    PieceType(int textureIndex, Color color, String glyph) { ... }

    public String texturePath() { return "resource/texture/chess/" + textureIndex + ".png"; }
    static PieceType random(RandomGenerator rng) { return values()[rng.nextInt(values().length)]; }
}
```

2. `Piece` 改为持有 `PieceType`，`getColor()` 从 `PieceType` 派生；删 `Constant.colorMap`。
3. `view.TileView`：删掉 `public static String[] chessTypes`，`paintComponent` 改为
   `piece.type().texturePath()` 直接取图，那 6 行循环查找整体消失。
4. `model.Chessboard`：把 `static import view.ChessComponent.chessTypes` 换成 `PieceType.random(bag)`。
5. `controller`：`import static view.ChessComponent.chessTypes` 删除，改引用 `PieceType.values()`。

**验收**：`grep -rn "^import.*view" src/model/` 空；`grep -rn "getName()" src/` 空；冒烟全绿。
**风险**：中。这是改动最广的一步（触达 model/view/controller 三层的棋子访问点），建议单独一个 commit。
**注意**：此处**有意保留** emoji 作为**显示用途**的 `glyph`，但不再作为身份判据 —— 身份一律用 `==` 比枚举。

---

## Step 3 —— 删除空壳 `Cell`

P3，纯减法。

**动作**
1. `Board` 的 `Cell[][] grid` → `Piece[][] grid`；删 `model/Cell.java`。
2. `getGridAt(point)` 返回 `Piece`；`initPieces` 里 `grid[i][j] = PieceType.random(...)`。
3. 受影响的外部引用只有 3 处：
   - `view.BoardView.initiateChessComponent` 的 `Cell[][] grid = ...` 两重循环；
   - `controller.doFallDown` 的 `model.getGrid()[r][c].getPiece() == null`；
   - `controller.doGenerateRandomPiecesOnTop` 的 `getGrid()[0][j].setPiece(...)`。
4. `view.CellView`（原 `CellComponent`）的 javadoc 里提到 `Cell` 的那句一并改掉。

**验收**：编译通过；`grep -rn "\bCell\b" src/model/` 空；冒烟全绿。
**风险**：低。`Cell implements Serializable` 从未被使用（` grep Serializable src/ ` 只有声明处），可直接删。

---

## Step 4 —— 值对象现代化（record 归位）

P4 收尾 + 坐标为一等公民。

**动作**
1. `BoardPoint`：删掉手写的 `equals` + `@SuppressWarnings("ALL")`（record 自动生成的等价实现已经一模一样），
   改为：
   ```java
   public record BoardPoint(int row, int col) {
       public int distanceTo(BoardPoint o) { return Math.abs(row-o.row()) + Math.abs(col-o.col()); }
       public List<BoardPoint> neighbors() { /* 上下左右，边界自动裁剪 */ }
       @Override public String toString() { return "(%d,%d)".formatted(row, col); }
   }
   ```
   `toString` 里那句 "on the chessboard is clicked!" 是 UI 文案，必须移出 model。
2. `Piece` → `record Piece(PieceType type)`（后续要加"冰块/炸弹"等状态时在这里扩，`type()` 还能提供 `color()` / `texturePath()` 的快捷转发）。
3. `Chessboard.calculateDistance` 这个 static 工具搬到 `BoardPoint.distanceTo`，`controller` 调用改为 `point.distanceTo(other)`。

**验收**：编译通过；`grep -rn "SuppressWarnings" src/model/` 空；冒烟全绿。
**风险**：低。唯一的注意点是 `MenuFrame`/`NetGame` 里若有根据 `toString()` 做字符串解析的地方要一并改（当前没有，可 grep 确认）。

---

## Step 5 —— 断开 model → controller 的循环依赖

P2。

**动作**
1. `Board.initPieces()` 里那三行 `isNewGameInitialized = false/true` 全部删除。理由：这个标志属于**会话生命周期**，不属于棋盘；而且它在 `initPieces()`（构造时也调用）里被写，导致"构造即副作用"。
2. 唯一消费方是 `NetGame.java:143` `while (!isNewGameInitialized)` 的忙等。改为显式回调：
   - 在 `listener.GameListener` 加 `void onGameReady()`（或直接返回 Board），
   - `GameController.initialize()` 末尾 `net.onGameReady()`，
   - `NetGame` 改事件驱动，删掉那个 `while(true) sleep(…) ` 轮询。
3. `GameController.isNewGameInitialized` 这个 `public static` 字段一并删除（它同时被 `Chessboard` 和 `NetGame` 读，是全局可变状态，删掉后 P2 彻底消失）。

**验收**：`grep -rn "isNewGameInitialized" src/` 空；联机建房/加入仍能开局（手动验证一次）。
**风险**：中低，但**只在实际联机路径生效**，本地冒烟测不到，需要手动点一次。建议这一步单独 commit 以便出问题时精准 revert。

---

## Step 6 —— 规则回迁：让 model 真正干活（**本计划的核心**）

P6/P8。目标：`GameController` 里那 ~250 行棋盘逻辑，全部下沉成 `Board` 的方法，
并且**以"变更事件列表"的形式返回**，让 view 不再需要偷看模型内部。

**动作 —— `Board` 新增 API**

```java
int rows(); int cols(); boolean contains(BoardPoint p);

Piece pieceAt(BoardPoint p);                    // 替代 getGrid()[r][c].getPiece()
void setPiece(BoardPoint p, Piece piece);       // 带 Objects.requireNonNull / contains 校验
Piece removePiece(BoardPoint p);
void swap(BoardPoint a, BoardPoint b);          // 要求 p.isAdjacent(q)

boolean hasEmptyCells();                        // ← controller.checkChessBoardHasEmpty

List<BoardPoint> findMatches();                 // ← controller.doChessEliminate 的扫描主体；>=3 横竖全返
int  clearMatches();                            // 清掉 findMatches() 的结果，返回得分增量

List<Move>  collapse();                         // ← controller.doFallDown；重力，返回每颗棋子的位移
List<Spawn> refill();                           // ← controller.doGenerateRandomPiecesOnTop

boolean wouldMatch(BoardPoint a, BoardPoint b); // 纯查询：swap 后是否成三连（不落副作用）
Optional<Swap> findAnyValidSwap();              // ← controller.isNotContinuable + hint 的合并版
void fillAll();                                 // 开局/洗牌：逐个拒绝直至无送分（替代整板重roll）
```

关键点说明：
- **以变更事件列表作为唯一出口**：`collapse()` 返回 `List<Move>`、`refill()` 返回 `List<Spawn>`，
  controller 拿到列表后逐条驱动 view 做下落动画 —— 这样 view 的动画顺序有了模型依据，
  不再靠 `view.getGridComponentAt(...).getComponent(0)` 去猜。
- **开局的"无送分"生成**：`fillAll()` 用**逐格拒绝采样**（某格会立即成三连就重抽），
  替换现在 `while(checkerBoardValidator(grid))` 的"整板推翻重来"，顺带去掉 try/catch 判空。
- `checkerBoardValidator` 这个名字语义反了，改名/删除：`findMatches().isEmpty()` 表达更清楚。
- `hint()` 的"带副作用的查询"改造为 `findAnyValidSwap()` 纯查询，controller 再据此设置选中态。

**controller 侧改造**：`doChessEliminate` / `doFallDown` / `doGenerateRandomPiecesOnTop` /
`checkChessBoardHasEmpty` / `isNotContinuable` / `isMatchable` 六个方法全部删除，
替换成对上述 API 的编排调用。`GameController` 应当明显变短。

**验收**：`GameController.java` 从 871 行降到 ~500 行以下；Step 0 四个冒烟断言全绿；
手动玩 5 分钟，确认：交换→消除→下落→连锁消除→补棋→計步/计分 都与重构前一致。
**风险**：**最高的一步**。因为它重写了核心算法（且要顺手改掉"整板重roll"和"忙循环"这类旧行为）。
建议拆成两个 commit：`6a` 原样搬迁（行为逐字节等价），`6b` 行为优化。出问题可按半步回滚。

---

## Step 7 —— 封住内部数组：删除 `getGrid()`

P7。

**动作**
1. `Board` 新增 `BoardSnapshot snapshot()`：返回一个 **不可变** 拷贝，供 view 一次性读取。
   ```java
   public record BoardSnapshot(int rows, int cols, List<List<Optional<PieceType>>> cells) {}
   ```
   （或者扁平化为 `PieceType[][]`，记得在 `snapshot()` 内做 `clone()`，返回侧保持只读。）
2. 删 `getGrid()`。受影响的外部引用：
   - `view.BoardView.initiateChessComponent` → 改吃 `BoardSnapshot`；
   - `controller` 里 6 处 `model.getGrid()[i][j]` → `model.pieceAt(p)`。
3. 提供 `Iterable<BoardPoint> points()` 让 view/controller 遍历时不必关心行宽。

**验收**：`grep -rn "getGrid()" src/` 空；冒烟全绿。
**风险**：低（Step 6 已经把大部分直接访问替换掉了，这步只是补漏+封口）。

---

## Step 8 —— 干掉 `Constant` 枚举 + 消灭硬编码 8

P5/P11。

**动作**
1. 删除 `model/Constant.java`。棋盘尺寸改为 **`Board` 的实例字段**，由构造注入：
   ```java
   public final class Board {
       public static final int DEFAULT_SIZE = 8;
       private final int rows, cols;
       public Board() { this(DEFAULT_SIZE, DEFAULT_SIZE); }
       public Board(int rows, int cols) { ... }   // 顺手解锁"不同难度不同棋盘"的可能性
   }
   ```
2. 受影响约 30 处 `Constant.DEFAULT_CHESSBOARD_ROW_SIZE.getNum()` → `board.rows()`。
   `controller` 里调用最密集，`view.BoardView` 里 `gridComponents` 数组与 `CHESS_SIZE*8` 改为读 `rows()/cols()`。
3. `loadFromFile` / `loadFromString` 里的 `new int[8][8]` 改为按 Board 实际尺寸。

**验收**：`grep -rn "Constant\.\|DEFAULT_CHESSBOARD" src/` 空；`grep -rn "\* 8\|\[8\]\[8\]" src/` 空；冒烟全绿。
**风险**：低，属于纯机械替换量大。建议用 IDE structural replace 而非手工。

---

## Step 9 —— 难度模型统一（消灭重复）

P9。

**动作**
1. `Difficulty` 改为 record，删掉手工 `equals`/`hashCode`（record 自带）：
   ```java
   public record Difficulty(String name, int goal, int stepLimit, int timeLimit) {
       public static final int UNLIMITED = -1;
       public boolean hasStepLimit() { return stepLimit > UNLIMITED; }
       public boolean hasTimeLimit() { return timeLimit > UNLIMITED; }
   }
   ```
   之前那套"构造函数里反向 equals 三个预设来决定名字"的逻辑整体删除 —— 名字是数据，不是推理结果。
2. `DifficultyPreset` 字段改 `private final`，加 accessor，并提供唯一转换出口：
   ```java
   public enum DifficultyPreset implements DifficultySpec {
       EASY(30, -1, -1), NORMAL(50, 30, 180), HARD(180, 46, 180);
       @Override public Difficulty difficulty() { return new Difficulty(name(), goal, stepLimit, timeLimit); }
   }
   ```
3. 删 `GameController.difficultyPresets` 这个 ArrayList —— 预设列表的事实来源是
   `DifficultyPreset.values()`，不需要再抄一份。
4. 删 `Difficulty.equals(DifficultyPreset)` 重载（重载 `equals` 极易引发静态分派陷阱），
   `loadFromFile` 里那段"逐个比回预设"的循环改为：
   ```java
   var preset = Arrays.stream(DifficultyPreset.values())
                      .filter(p -> p.difficulty().goal() == goal && ...)
                      .findFirst();
   ```
   或更简单：存档直接写难度名 `EASY`，加载时 `DifficultyPreset.valueOf(name).difficulty()`。

**验收**：`grep -rn "difficultyPresets" src/` 空；三个预设 + 自定义难度的标签显示与旧行为一致。
**风险**：中低。主要是 `MenuFrame.difficulty` 是 `public static` 全局可变，改签名后会牵出一串；建议本步内保持字段名不变，只改类型内部结构。

---

## Step 10 —— 存档格式：独立 codec + 版本化（顺手修 3 个 bug）

P10。

**动作**
1. 新增 `model/GameState.java`：
   ```java
   public record GameState(int score, int timeLeft, int stepLeft, Difficulty difficulty, BoardSnapshot board) {}
   ```
2. 新增 `model/GameStateCodec.java`，承载当前散在 `GameController` 里的 4 段近乎重复的 IO/解析：
   - `static String toText(GameState)` ← 合并 `saveToFile` 与 `ConvertToString`（两者现在只差一个尾部空格）；
   - `static Optional<GameState> fromText(String)` ← 合并 `loadFromFile` 与 `loadFromString`，
     返回 `Optional` 而非到处 `JOptionPane` 弹窗（**UI 告警留在 controller**）；
   - 首行加版本头：`M3SAVE 2`，解析时按版本分支，为以后改棋盘尺寸留后路。
3. 新增 `model/GameRepository.java`：`load(Path)` / `save(Path, GameState)`，
   controller 里就只剩 `try { var state = repo.load(p); onLoaded(state); }
   catch (IOException e) { showError(...); }` 这么干净。
4. 顺手修的 bug：
   - `Math.clamp(idx, 0, chessTypes.length)` → 上界应为 `PieceType.values().length - 1`（现在会越界到非法棋子）；
   - 缺值兜底 `new Random().nextInt(4)` → `PieceType.random(bag)`（4 与 6 种棋子对不上）；
   - 写入按 `[i][j]`、读取按 `[j][i]` 的行列转置不一致 —— 在 codec 里定死一个方向并加注释说明；
   - `getFileExtension` 从 model 挪到 `GameRepository`，或直接用 `Path`/正则替代。

**验收**：用现成的 `savedGame.txt` 与 `test/deadend.txt` **原样加载成功**且局面与旧版一致（这是最有说服力的回归证据）；
再加新增的 `saveLoadRoundTrip` 冒烟断言。
**风险**：中。存档兼容性是这个 forks 里最容易翻车的点 —— 建议保留**旧格式读取分支**（只读不写），跑通一轮后再决定是否删除。

---

## Step 11 —— 收尾清理 + 现代语法

P12。放在最后，纯收益项。

**动作**
1. **删掉 `model/Util.java`**：`RandomPick` 已被 Step 0 的 `PieceBag` 取代；
   `getFileExtension` 已归于 `GameRepository`。注意 `view.TileView:70` 还在用它随机选指针贴图，一并改为注入 rng。
2. 用上当前语言级别（IDEA 配的是 **JDK_27**，本机 JDK 是 **25.0.4**，建议统一）：
   - `switch` 表达式替代 `if-else` 链（如难度选择 `DifficultySelectFrame`）；
   - pattern matching for `switch`（替换各处 `(Type) x` 裸强转 + `instanceof` 组合）；
   - record patterns、`var`、`SequencedCollection`（`List.getFirst/getLast`）；
   - `instanceof` 模式变量替换 `(ChessComponent) x.getComponent(0)` 这类裸强转。
3. 把遗留的 `System.out.println` 调试输出收敛为一个统一的日志出口（或直接删掉
   `System.out.print("")` 这种空输出 —— 它在 `GameController` 里有两处）。
4. 清掉注释掉的代码块（`Chessboard:47`、`DifficultySelectFrame:105-109`、`TileView:48-50` 那三家 FIXME/TODO）。
   > 提醒：`TileView` 里那条 `// FIXME: make the image translucent` 是**真实待办**不是死代码，
   > 删除前请确认，或者清到 issue 里而不是直接删。

**验收**：编译无警告；`grep -rn "TODO\|FIXME\|System.out.print(\"\")" src/` 空。
**风险**：低。唯一建议：**`System.out.println` 的清理单独一个 commit**，否则 diff 会被大量删行淹没，review 困难。

---

## Step 12 —— 可选项：移除 `view` 中的全局静态状态

严格说这超出了 "model 精简"，但它是 model 无法彻底干净的根源之一，列在这里供取舍。

现状：`MenuFrame.difficulty`（`public static`）、`MenuFrame.isDarkMode`、`MenuFrame.isDetailedDialog`、
`ChessGameFrame.isOnlinePlay`、`GameController.isAutoRestart` —— **游戏会话状态散落在 5 个类的 static 字段上**，
model 想拿到自己的配置只能反向读 view，这就是为什么 Step 5 的循环依赖会存在。

建议引入 `model.GameConfig`（或者会话级的 `GameSession`）record 把它们集中起来，由 `GameFrame` 持有并注入。
**可以不做**：如果课程验收只到"能跑"，这步性价比一般；如果要继续做网络对战或多人同机对局，这步非做不可。

---

## 批处理建议与优先级

| 建议顺序 | 组合 | 理由 |
|---|---|---|
| **第一批** | Step 0 → 1 → 2 → 3 → 4 | 结构与数据表示定型，风险可控，收益（依赖单向、类型安全）立刻可见 |
| **第二批** | Step 5 → 6a（原样搬迁） | 行为等价的规则回迁，是本计划真正值钱的部分 |
| **第三批** | Step 6b（算法优化） → 7 → 8 | 在此之上做优化与封装收口 |
| **第四批** | Step 9 → 10 → 11 | 难度/存档/杂项，独立性最强，随时可停 |

**可以随时停下来、且代码依然干净自洽的节点**：Step 2 之后（依赖已单向）、Step 6a 之后（逻辑已归位）、
Step 7 之后（封装已闭合）。其余步骤之间都是松耦合，跳过任何一步都不影响其他步骤的编译。

**每步统一的验收动作**
```bash
javac -d out $(find src -name '*.java') && java -cp out smoke.SmokeTest
# 再手动跑一轮：开局 → 交换 → 消除 → 下落 → 连锁 → 存档 → 读档 → 洗牌
```

**代码注释约定**（沿用既有偏好）：本轮重构的注释与 commit message 只写
"改成了什么"，不写"为什么当初是那样"。随着这次清理删掉那些 `// for debug`、`// a workaround` 的注释，
注释密度会自然下降 —— 这是正确的方向。
