public class Btree {

    public static TreeNode search(TreeNode node, int key) {
        if (node.isLeaf) return null;

        // 노드의 키 중, 검색할 Key보다 작은 것중 가장 큰 키의 인덱스를 찾는다.
        int keyIndex = 0;
        while (keyIndex < node.currentKeyCount && key > node.keys[keyIndex]) {
            keyIndex++;
        }

        // 검색할 Key보다 작은 것중 가장 큰 키가 검색할 Key와 같은지 확인한다.
        while(keyIndex < node.currentKeyCount && key == node.keys[keyIndex]) {
            return node;
        }

        return search(node.children[keyIndex], key);
    }
}
