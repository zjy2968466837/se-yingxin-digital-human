"""
简易MLP多层感知机功能Demo
功能：模拟全连接神经网络前向计算，手写数字10分类
"""
import numpy as np

class SimpleMLP:
    def __init__(self, input_size=784, hidden_size=128, output_size=10):
        # 初始化权重
        self.w1 = np.random.randn(input_size, hidden_size) * 0.01
        self.b1 = np.zeros((1, hidden_size))
        self.w2 = np.random.randn(hidden_size, output_size) * 0.01
        self.b2 = np.zeros((1, output_size))

    def relu(self, x):
        return np.maximum(0, x)

    def softmax(self, x):
        exp_x = np.exp(x - np.max(x, axis=1, keepdims=True))
        return exp_x / np.sum(exp_x, axis=1, keepdims=True)

    def forward(self, x):
        """前向传播"""
        h1 = self.relu(np.dot(x, self.w1) + self.b1)
        out = self.softmax(np.dot(h1, self.w2) + self.b2)
        return out

    def predict(self, x):
        pred = self.forward(x)
        return np.argmax(pred, axis=1)


if __name__ == "__main__":
    # 模拟一张28*28手写数字图片向量 (784维)
    test_img = np.random.rand(1, 784)
    model = SimpleMLP()
    result = model.predict(test_img)
    print(f"MLP预测手写数字类别：{result[0]}")
    print("✅ MLP前向推理功能执行完成")
