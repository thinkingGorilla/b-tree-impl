import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class BTree {

    // 최소차수로 구성할 때 이점?
    // 1. 항상 홀수 개의 키 → 항상 수학적 정중앙이 존재
    // 2. 대칭적 분할 가능 →
    //   키에서 중앙값 하나를 부모로 올리면 남은 키의 개수는 2t-2로,
    //   이를 절반으로 나누면 정확히 t-1개씩 양쪽에 배분 가능하다.
    final int minDegree;
    BTreeNode root;

    public BTree(int minDegree) {
        if (minDegree < 2) throw new IllegalArgumentException("minDegree must be >= 2");

        this.minDegree = minDegree;
        this.root = new BTreeNode(minDegree, true);
    }

    public void printBTree() {
        if (nonNull(root)) root.printInOrder();
        System.out.println();
    }

    // [ 오름차순 정렬의 원칙 ]
    // 부모 노드에 올라간 키 K를 기준으로,
    // 왼쪽 자식들은 K보다 작아야 하고 오른쪽 자식들은 K보다 커야 한다.
    // 제자리(in-place) 알고리즘으로 배열 삽입을 실행한다.
    // 관계 중심 표기법 : C₀, K₀, C₁, K₁, C₂, …, Cₙ, Kₙ, Cₙ₊₁
    // 배열 중심 표기법 : C₀, K₀, C₁, K₁, C₂, K₂, …, Kₙ₋₁, Cₙ
    // parent.keys[i]를 기준으로,
    // 왼쪽 자식은 parent.children[i]
    // 오른쪽 자식은 parent.children[i + 1]
    // childIndex를 기준으로,
    // parent.children[childIndex]의 부모키는 parent.keys[childIndex]
    // parent.children[childIndex + 1]의 부모키는 parent.keys[childIndex]
    void splitChild(BTreeNode parent, int childIndex) {
        BTreeNode fullChild = parent.children[childIndex];
        BTreeNode newChild = new BTreeNode(minDegree, fullChild.isLeaf);

        // 키 목록의 오른쪽 절반을 복사한다.
        // 꽉 찬 노드에는 (2*t)-1 개의 키가 있다.
        // 이중 1개는 부모로 올라간다.
        // 따라서 남은 노드 키의 개수는 2t-2 개 이다.
        // 2t-2를 둘로 나누면 t-1개씩 왼쪽과 오른쪽이 나눠 가진다.
        // 따라서 오른쪽 노드에 복사할 개수는 t-1개이다.
        // 왼쪽 노드 : 0 ~ t-2 → t-2 - 0 + 1 = t-1 개
        // 부모로 올라갈 값 : t-1
        // 오른쪽 노드 : t ~ 2t-2 → 2t-2 - t + 1 = t-1 개
        // 오른쪽 노드는 minDegree(=t)부터 시작해서 끝까지 복사해야 한다.
        for (int i = 0; i < minDegree - 1; i++) {
            newChild.keys[i] = fullChild.keys[minDegree + i];
        }

        if (!fullChild.isLeaf) {
            for (int i = 0; i < minDegree; i++) {
                newChild.children[i] = fullChild.children[minDegree + i];
            }
        }

        // 키에서 중앙값 하나를 부모로 올리면 남은 키의 개수는 2t-2로
        // 이를 절반으로 나누면 정확히 t-1개씩 양쪽에 배분된다.
        fullChild.keyCount = minDegree - 1;
        newChild.keyCount = minDegree - 1;

        // 부모의 자식 포인터 이동
        // 제자리(in-place) 알고리즘으로 새로운 노드의 포인터가 들어갈 위치를 확보한다.
        // childIndex 위치의 키는 그대로 두고,
        // 다음 인덱스(childIndex + 1)부터 끝까지의 데이터들을 오른쪽으로 밀어야 한다.
        // 내려가면서 미리 꽉 찬 노드를 쪼개는(Proactive Split) 방식을 쓰기 때문에,
        // index out of bounds가 발생할 일은 없다.
        // e.g. ◼️◼️◼️◽️◽️ → ◼️◽️◼️◼️◽️
        // 새 노드는 정렬 규칙에 따라, 분할된 노드의 오른쪽 위치에 삽입되어야 한다.
        // B-Tree에서 자식 개수는 항상 (키 개수 + 1)이므로,
        // parent.keyCount는 현재 유효한 마지막 자식의 인덱스를 의미한다.
        // 따라서 자식 배열 이동은 parent.keyCount부터 시작해야한다.
        for (int i = parent.keyCount; i >= childIndex + 1; i--) {
            parent.children[i + 1] = parent.children[i];
        }
        // 새 자식 연결
        // C₀, K₀, C₁, K₁, C₂, K₂, …, Kₙ₋₁, Cₙ
        parent.children[childIndex + 1] = newChild;

        // 부모의 키 이동
        // 현재 유효한 마지막 키의 인덱스는 keyCount - 1이다.
        for (int i = parent.keyCount - 1; i >= childIndex; i--) {
            parent.keys[i + 1] = parent.keys[i];
        }
        // 꽉 찬 노드의 중앙 키 승격
        // C₀, K₀, C₁, K₁, C₂, K₂, …, Kₙ₋₁, Cₙ
        parent.keys[childIndex] = fullChild.keys[minDegree - 1];
        parent.keyCount++;
    }

    void insert(int key) {
        // 루트 노드가 꽉 찼는지 확인한다.
        if (root.keyCount == (2 * minDegree) - 1) {
            // B-Tree는 항상 내려가기 전에 "자식" 노드가 가득 차 있다면 분할해야한다.
            // 하지만 루트는 부모 노드가 없으므로 부모 노드를 만들어 연결한다.
            BTreeNode newRoot = new BTreeNode(minDegree, false);
            newRoot.children[0] = root;
            root = newRoot;

            // 꽉 찬 노드(= 기존 루트 노드)를 분할하고 중앙 키를 새 루트로 올린다.
            splitChild(root, 0);
            // 분할이 되었으므로 새 루트 노드를 기준으로 삽입 탐색을 진행한다.
            insertNonFull(root, key);
        } else {
            insertNonFull(root, key);
        }
    }

    void insertNonFull(BTreeNode node, int key) {
        // 키 배열을 비교 대상으로 삼기 때문에, 마지막 유효 키 인덱스인 keyCount - 1부터 시작한다.
        int keyIndex = node.keyCount - 1;
        if (node.isLeaf) {
            // 전체 코드에서 키 또는 자식을 뒤에서부터 탐색하는 이유는
            // 삽입 위치 찾기와 요소 이동을 한 번에 처리하기 위해서이다.
            // 즉 데이터가 이동하는 방향의 반대쪽에서부터 시작한다.
            // 정렬된 배열 키 요소보다 키가 크다면 종료
            while (keyIndex >= 0 && key < node.keys[keyIndex]) {
                node.keys[keyIndex + 1] = node.keys[keyIndex];
                keyIndex--;
            }
            // 키 탐색이 종료된 시점의 keyIndex는 삽입하려는 키보다 작은 마지막 키의 위치를 가리킨다.
            // 따라서 실제로 내려가거나 삽입해야 할 위치는 그 오른쪽이므로 keyIndex++를 수행한다.
            node.keys[keyIndex + 1] = key;
            node.keyCount++;
        } else {
            // 키보다 작은 정렬된 배열 키 요소의 인덱스 탐색
            while (keyIndex >= 0 && key < node.keys[keyIndex]) {
                keyIndex--;
            }
            // 키 탐색이 종료된 시점의 keyIndex는 삽입하려는 키보다 작은 마지막 키의 위치를 가리킨다.
            // 따라서 실제로 내려가거나 삽입해야 할 위치는 그 오른쪽이므로 keyIndex++를 수행한다.
            keyIndex++;

            // 탐색할 자식 노드가 꽉 찬 상태라면 분할을 수행한다.
            if (node.children[keyIndex].keyCount == (2 * minDegree) - 1) {
                splitChild(node, keyIndex);

                // 분할 후 내려갈 방향을 재결정한다.
                // 자식 노드를 분할하면 중앙 키가 부모의 keyIndex 위치에 삽입된다.
                // 따라서 삽입하려는 키와 승격된 중앙 키를 비교하여,
                // 중앙 키가 크다면 현재 경로를, 중앙 키가 작다면 중앙 키의 우측 경로를 탐색할 수 있도록 한다.
                // e.g. [10, 50], key = 35
                // 자식에서 40이 올리온 경우 키 배열이 [10, 40, 50]이 되므로 분할 전 경로, 즉 키 10의 우측 자식 노드를 그대로 탐색
                // 자식에서 30이 올라온 경우 키 배열이 [10, 30, 50]이 되므로 30보다 큰 노드가 있는 키 30의 우측 자식 노드를 탐색
                if (key > node.keys[keyIndex]) {
                    keyIndex++;
                }
            }

            // 리프 노드에 다다를 때까지 재귀를 수행한다.
            insertNonFull(node.children[keyIndex], key);
        }
    }

    public void remove(int key) {
        if (isNull(root)) return;

        remove(root, key);

        if (root.keyCount == 0) {
            // 루트 노드 하나만 남아 있는 상태에서 계속 삭제한 경우
            if (root.isLeaf) {
                root = null;
            // 분할 후 merge가 발생하여 루트의 키 카운트가 0이된 경우
            } else {
                root = root.children[0];
            }
        }
    }

    private void remove(BTreeNode node, int key) {
        int index = findKey(node, key);

        // 키가 현재 노드에 존재하는 경우
        // 키 카운트를 넘는 경우 → 오른쪽 자식 노드에 키가 있을 가능성
        // 찾는 인덱스의 키 요소와 키 값이 다른 경우 → children[index] 노드에 키가 있을 가능성
        // C₀, K₀, C₁, K₁, C₂, …, Cₙ, Kₙ, Cₙ₊₁
        // e.g. [10, 20, 30]
        // key = 5, findKey → 0
        // key = 15, findKey → 1
        // key = 25, findKey → 2
        // key = 35, findKey → 3
        // findKey가 out of index를 반환한 경우(= 키 카운트와 동일한 값)를 방어하기 위해
        // "index < node.keyCount" 조건을 넣는다.
        if (index < node.keyCount && node.keys[index] == key) {

        // key가 현재 노드에 없는 경우
        } else {

        }
    }

    // node.keys[] 배열에서 키가 있거나, 키가 들어가야 할 위치를 반환한다.
    int findKey(BTreeNode node, int key) {
        int index = 0;
        // 키에 가장 근접한 키 요소의 인덱스를 반환한다.
        // e.g. [10, 20, 30], key = 50
        // index 0 → 10 < 50, index++
        // index 1 → 20 < 50, index++
        // index 2 → 30 < 50, index++
        // index 3 → 3 < 3, "키 카운트와 동일한 값"이므로 종료
        while (index < node.keyCount && node.keys[index] < key) {
            index++;
        }

        return index;
    }


    public static class BTreeExample {

        public static void main(String[] args) {
            BTree bTree = new BTree(6);

            bTree.insert(10);
            bTree.insert(20);
            bTree.insert(5);
            bTree.insert(6);


            System.out.print("In-order traversal of the B-tree: ");
            bTree.printBTree();
        }
    }
}
