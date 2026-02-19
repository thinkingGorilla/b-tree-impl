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

        // 1. 키 목록의 오른쪽 절반을 복사한다.
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
        fullChild.keysCount = minDegree - 1;
        newChild.keysCount = minDegree - 1;

        // 부모의 자식 포인터 이동
        // 제자리(in-place) 알고리즘으로 새로운 노드의 포인터가 들어갈 위치를 확보한다.
        // childIndex 위치의 키는 그대로 두고,
        // 다음 인덱스(childIndex + 1)부터 끝까지의 데이터들을 오른쪽으로 밀어야 한다.
        // 내려가면서 미리 꽉 찬 노드를 쪼개는(Proactive Split) 방식을 쓰기 때문에,
        // index out of bounds가 발생할 일은 없다.
        // e.g. ◼️◼️◼️◽️◽️ → ◼️◼️◽️◼️◽️
        // 자식은 키보다 하나 더 많다.
        // 예를 들어 keyCount가 3일 떄, 자식은 키보다 하나 더 많다는 점을 생각하면
        // 자식은 배열 인덱스 3까지 차 있다. 따라서 인덱스의 시작점은 keyCount부터 시작한다.
        for (int i = parent.keysCount; i >= childIndex + 1; i--) {
            parent.children[i + 1] = parent.children[i];
        }
        // 새 자식 연결
        // C₀, K₀, C₁, K₁, C₂, K₂, …, Kₙ₋₁, Cₙ
        parent.children[childIndex + 1] = newChild;

        // 부모의 키 이동
        // 이동은 키의 길이(= keyCount)보다 하나 작은 횟수만큼 수행되어야 한다.
        for (int i = parent.keysCount - 1; i >= childIndex; i--) {
            parent.keys[i + 1] = parent.keys[i];
        }
        // 꽉 찬 노드의 중앙 키 승격
        // C₀, K₀, C₁, K₁, C₂, K₂, …, Kₙ₋₁, Cₙ
        parent.keys[childIndex] = fullChild.keys[minDegree - 1];
        parent.keysCount++;
    }

    void insert(int key) {
        // root 처리
    }

    void insertNonFull(BTreeNode node, int key) {
        int childIndex = node.keysCount - 1;
        if (node.isLeaf) {
            // 정렬 유지 삽입
            // 제자리(in-place) 알고리즘으로 배열 삽입
            // 정렬된 배열 키 요소보다 키가 크다면 종료
            while (childIndex >= 0 && key < node.keys[childIndex]) {
                node.keys[childIndex + 1] = node.keys[childIndex];
                childIndex--;
            }
            // 정렬된 배열 키 요소보다 크다고 확인한 요소 다음 인덱스 위치에 키 삽입
            node.keys[childIndex + 1] = key;
            node.keysCount++;
        } else {
            // 키보다 작은 정렬된 배열 키 요소의 인덱스 탐색
            while (childIndex >= 0 && key < node.keys[childIndex]) {
                childIndex--;
            }
            // 오름차순으로 정렬되어야 하므로,
            // 키보다 작은 정렬된 배열 키의 우측노드에 삽입해야 한다.
            childIndex++;

            // 탐색할 자식 노드가 꽉 찬 상태라면 분할을 수행한다.
            if (node.children[childIndex].keysCount == (2 * minDegree) - 1) {
                splitChild(node, childIndex);

                // 분할 후 내려갈 방향을 재결정한다.
                // 자식에서 올라온 중앙 키와 키를 비교하여 중앙 키가 크다면 현재 경로를,
                // 중앙 키가 작다면 중앙 키의 우측 경로를 탐색할 수 있도록 한다.
                // e.g. [10, 50], key = 35
                // 자식에서 40이 올리온 경우 [10, 40, 50]이 되므로 분할 전 경로, 즉 10의 우측 노드를 그대로 탐색
                // 자식에서 30이 올라온 경우 [10, 30, 50]이 되므로 30보다 큰 노드가 있는 30의 우측 노드를 탐색
                if (key > node.keys[childIndex]) {
                    childIndex++;
                }
            }

            // 리프 노드에 다다를 때까지 재귀를 수행한다.
            insertNonFull(node.children[childIndex], key);
        }
    }

    public static void main(String[] args) {
        BTree bTree = new BTree(6);

        bTree.insert(10);
        bTree.insert(20);
        bTree.insert(5);
        bTree.insert(6);


        System.out.print("In-order traversal of the B-tree: ");
        // bTree.traverse();
    }
}
