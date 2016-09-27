package imagecomparator;

public class BlobPixel {
	private BlobPixel top, right, bottom, left;
	private int blobSize = 1;
	
	public BlobPixel(BlobPixel top, BlobPixel right, BlobPixel bottom, BlobPixel left)
	{
		this(top, right, bottom, left, 1);
	}
	
	public BlobPixel(BlobPixel top, BlobPixel right, BlobPixel bottom, BlobPixel left, int blobSize)
	{
		this.top = top;
		if(this.top != null)
		{
			this.top.setBottom(this);
		}
		this.right = right;
		if(this.right != null)
		{
			this.right.setLeft(this);
		}
		this.bottom = bottom;
		if(this.bottom != null)
		{
			this.bottom.setTop(this);
		}
		this.left = left;
		if(this.left != null)
		{
			this.left.setRight(this);
		}
		this.blobSize = blobSize;
	}

	public BlobPixel getTop() {
		return top;
	}

	public void setTop(BlobPixel top) {
		this.top = top;
	}

	public BlobPixel getRight() {
		return right;
	}

	public void setRight(BlobPixel right) {
		this.right = right;
	}

	public BlobPixel getBottom() {
		return bottom;
	}

	public void setBottom(BlobPixel bottom) {
		this.bottom = bottom;
	}

	public BlobPixel getLeft() {
		return left;
	}

	public void setLeft(BlobPixel left) {
		this.left = left;
	}

	public int getBlobSize() {
		return blobSize;
	}

	public void setBlobSize(int blobSize) {
		this.blobSize = blobSize;
	}
}
